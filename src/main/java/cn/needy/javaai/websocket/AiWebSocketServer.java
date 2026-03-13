package cn.needy.javaai.websocket;

import cn.needy.javaai.assistant.MediBuddyAgent;
import cn.needy.javaai.bean.ChatForm;
import cn.needy.javaai.security.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiWebSocketServer {

    private final MediBuddyAgent mediBuddyAgent;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    @Value("${ai.websocket.port:9000}")
    private int port;

    private WebSocketServer server;
    // 并发安全的连接映射：userId -> WebSocket
    private final ConcurrentHashMap<Long, WebSocket> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void start() {
        // 启动独立的 WebSocket 服务器（与 HTTP 端口隔离）
        server = new WebSocketServer(new InetSocketAddress(port)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                // 连接建立时校验 JWT，失败直接断开
                Long userId = authenticate(handshake);
                if (userId == null) {
                    conn.close(1008, "Unauthorized");
                    return;
                }
                conn.setAttachment(userId);
                // 同一用户只保留一个连接，新的连接会替换旧连接
                WebSocket previous = sessions.put(userId, conn);
                if (previous != null && previous != conn) {
                    previous.close(1000, "Replaced by new connection");
                }
                log.info("AI WebSocket connection opened: userId={} addr={}", userId, conn.getRemoteSocketAddress());
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                Long userId = conn.getAttachment();
                if (userId != null) {
                    // 断开时清理映射，避免内存泄漏
                    sessions.remove(userId, conn);
                }
                log.info("AI WebSocket connection closed: userId={} addr={} reason={}",
                        userId, conn.getRemoteSocketAddress(), reason);
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                handleMessage(conn, message);
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                if (conn != null) {
                    log.warn("AI WebSocket error on {}: {}", conn.getRemoteSocketAddress(), ex.getMessage());
                } else {
                    log.warn("AI WebSocket server error: {}", ex.getMessage());
                }
            }

            @Override
            public void onStart() {
                log.info("AI WebSocket server started on port {}", port);
            }
        };
        server.start();
    }

    @PreDestroy
    public void stop() {
        if (server == null) {
            return;
        }
        try {
            server.stop(1000);
            log.info("AI WebSocket server stopped");
        } catch (Exception e) {
            log.warn("Failed to stop AI WebSocket server: {}", e.getMessage());
        }
    }

    private void handleMessage(WebSocket conn, String message) {
        Long userId = conn.getAttachment();
        if (userId == null) {
            conn.close(1008, "Unauthorized");
            return;
        }
        // 入参 JSON -> ChatForm（与 HTTP 复用同一请求结构）
        ChatForm form;
        try {
            form = objectMapper.readValue(message, ChatForm.class);
        } catch (Exception e) {
            send(conn, AiChatResponse.error(null, "Invalid JSON payload"));
            return;
        }

        if (form.getMemoryId() == null) {
            send(conn, AiChatResponse.error(null, "memoryId is required"));
            return;
        }
        if (form.getUserMessage() == null || form.getUserMessage().isBlank()) {
            send(conn, AiChatResponse.error(form.getMemoryId(), "userMessage is required"));
            return;
        }

        try {
            String reply = mediBuddyAgent.chat(String.valueOf(form.getMemoryId()), form.getUserMessage());
            WebSocket target = sessions.get(userId);
            if (target != null) {
                // 按 userId 查找连接并推送结果
                send(target, AiChatResponse.ok(form.getMemoryId(), reply));
            }
        } catch (Exception e) {
            send(conn, AiChatResponse.error(form.getMemoryId(), "AI service error"));
            log.warn("AI chat failed for memoryId={}: {}", form.getMemoryId(), e.getMessage());
        }
    }

    private void send(WebSocket conn, AiChatResponse response) {
        try {
            conn.send(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            conn.send("{\"error\":\"Serialization error\"}");
        }
    }

    private Long authenticate(ClientHandshake handshake) {
        // 允许通过 Header 或 URL 参数携带 Token
        String token = resolveToken(handshake);
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return jwtService.extractUserId(token);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveToken(ClientHandshake handshake) {
        String authHeader = handshake.getFieldValue("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        String resource = handshake.getResourceDescriptor();
        return getQueryParam(resource, "token");
    }

    private String getQueryParam(String resource, String key) {
        if (resource == null) {
            return null;
        }
        int idx = resource.indexOf('?');
        if (idx < 0 || idx >= resource.length() - 1) {
            return null;
        }
        String query = resource.substring(idx + 1);
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && key.equals(parts[0])) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Data
    @AllArgsConstructor
    private static class AiChatResponse {
        private Long memoryId;
        private String assistantMessage;
        private String error;

        static AiChatResponse ok(Long memoryId, String assistantMessage) {
            return new AiChatResponse(memoryId, assistantMessage, null);
        }

        static AiChatResponse error(Long memoryId, String error) {
            return new AiChatResponse(memoryId, null, error);
        }
    }
}
