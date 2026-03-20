package cn.needy.medibuddy.websocket;

import cn.needy.medibuddy.ai.assistant.MediBuddyAgent;
import cn.needy.medibuddy.bean.ChatForm;
import cn.needy.medibuddy.security.JwtService;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    // 每个连接仅保留一个活动流
    private final Map<WebSocket, StreamContext> activeStreams = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

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
                closeActiveStream(conn, "connection closed");
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
        } finally {
            scheduler.shutdownNow();
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

        // 文档式协议断言：start -> delta* -> end，error 终止且不发送 end
        closeActiveStream(conn, "replaced by new request");
        StreamContext context = new StreamContext(conn, form.getMemoryId());
        activeStreams.put(conn, context);

        WebSocket target = sessions.get(userId);
        if (target == null) {
            closeActiveStream(conn, "connection missing");
            return;
        }

        send(target, AiChatResponse.start(form.getMemoryId()));
        scheduleFlush(context);

        try {
            mediBuddyAgent.streamChat(String.valueOf(form.getMemoryId()), form.getUserMessage())
                    .onPartialResponse(token -> {
                        if (context.closed.get()) {
                            return;
                        }
                        synchronized (context.buffer) {
                            context.buffer.append(token);
                            context.receivedChars += token.length();
                        }
                    })
                    .onCompleteResponse(response -> {
                        if (context.closed.compareAndSet(false, true)) {
                            // 先取消定时刷新，避免重复发送
                            cancelFlush(context);
                            // 直接排空 buffer，不经过 flushBuffer 的 closed 守卫
                            String remaining;
                            synchronized (context.buffer) {
                                remaining = context.buffer.toString();
                                context.buffer.setLength(0);
                            }
                            if (!remaining.isEmpty()) {
                                send(target, AiChatResponse.delta(form.getMemoryId(), remaining));
                            }
                            // 兜底：如果流式回调一个 token 都没收到，用完整响应补发
                            if (remaining.isEmpty() && context.sentChars == 0) {
                                String finalText = null;
                                if (response != null && response.aiMessage() != null) {
                                    finalText = response.aiMessage().text();
                                }
                                if (finalText != null && !finalText.isBlank()) {
                                    send(target, AiChatResponse.delta(form.getMemoryId(), finalText));
                                }
                            }
                            send(target, AiChatResponse.end(form.getMemoryId()));
                        }
                    })
                    .onError(error -> {
                        if (context.closed.compareAndSet(false, true)) {
                            send(target, AiChatResponse.error(form.getMemoryId(), "AI service error"));
                            cancelFlush(context);
                        }
                        log.warn("AI streaming failed for memoryId={}: {}", form.getMemoryId(), error.getMessage());
                    })
                    .start();
        } catch (Exception e) {
            if (context.closed.compareAndSet(false, true)) {
                send(target, AiChatResponse.error(form.getMemoryId(), "AI service error"));
                cancelFlush(context);
            }
            log.warn("AI stream start failed for memoryId={}: {}", form.getMemoryId(), e.getMessage());
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

    private void scheduleFlush(StreamContext context) {
        long delayMs = 500 + ThreadLocalRandom.current().nextInt(0, 101);
        context.flushFuture = scheduler.schedule(() -> {
            flushBuffer(context);
            if (!context.closed.get()) {
                scheduleFlush(context);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private void flushBuffer(StreamContext context) {
        if (context.closed.get()) {
            return;
        }
        String delta;
        synchronized (context.buffer) {
            if (context.buffer.isEmpty()) {
                return;
            }
            delta = context.buffer.toString();
            context.buffer.setLength(0);
            context.sentChars += delta.length();
        }
        send(context.conn, AiChatResponse.delta(context.memoryId, delta));
    }

    private void cancelFlush(StreamContext context) {
        ScheduledFuture<?> future = context.flushFuture;
        if (future != null) {
            future.cancel(false);
        }
    }

    private void closeActiveStream(WebSocket conn, String reason) {
        StreamContext context = activeStreams.remove(conn);
        if (context == null) {
            return;
        }
        if (context.closed.compareAndSet(false, true)) {
            cancelFlush(context);
            log.info("AI stream closed: memoryId={} reason={}", context.memoryId, reason);
        }
    }

    @Data
    @AllArgsConstructor
    private static class AiChatResponse {
        private String type; // start|delta|end|error
        private Long memoryId;
        private String assistantMessage;
        private String error;

        static AiChatResponse start(Long memoryId) {
            return new AiChatResponse("start", memoryId, null, null);
        }

        static AiChatResponse delta(Long memoryId, String assistantMessage) {
            return new AiChatResponse("delta", memoryId, assistantMessage, null);
        }

        static AiChatResponse end(Long memoryId) {
            return new AiChatResponse("end", memoryId, null, null);
        }

        static AiChatResponse error(Long memoryId, String error) {
            return new AiChatResponse("error", memoryId, null, error);
        }
    }

    private static class StreamContext {
        private final WebSocket conn;
        private final Long memoryId;
        private final StringBuilder buffer = new StringBuilder();
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private volatile ScheduledFuture<?> flushFuture;
        private int receivedChars = 0;
        private int sentChars = 0;

        private StreamContext(WebSocket conn, Long memoryId) {
            this.conn = conn;
            this.memoryId = memoryId;
        }
    }
}
