package cn.needy.medibuddy.ai.store;

import cn.needy.medibuddy.bean.ChatSession;
import cn.needy.medibuddy.bean.SessionMessage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MongoChatMemoryStore implements ChatMemoryStore {

    private final MongoTemplate mongoTemplate;

    /**
     * 记录每次 getMessages 返回的消息数量，
     * 供 updateMessages 做差量计算。
     * key = memoryId
     */
    private final Map<Object, Integer> lastReturnedCount = new ConcurrentHashMap<>();

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        ChatSession session = findSession(memoryId);
        if (session == null) {
            lastReturnedCount.put(memoryId, 0);
            return new LinkedList<>();
        }

        List<ChatMessage> result = new LinkedList<>();

        if (session.getSystemMessage() != null && !session.getSystemMessage().isBlank()) {
            result.add(SystemMessage.from(session.getSystemMessage()));
        }

        // 摘要以 user/ai 伪对话形式注入，不占用 SystemMessage 位置
        if (session.getSummary() != null && !session.getSummary().isBlank()) {
            result.add(UserMessage.from("帮我回顾一下之前的对话内容"));
            result.add(AiMessage.from(session.getSummary()));
        }

        for (SessionMessage msg : session.getMessages()) {
            if (!msg.isSummarized()) {
                result.add(toLangChainMessage(msg));
            }
        }

        lastReturnedCount.put(memoryId, result.size());
        return result;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        ChatSession session = findSession(memoryId);
        boolean isNew = (session == null);

        if (isNew) {
            session = new ChatSession();
            session.setSessionId(memoryId.toString());
            session.setCurrentRound(0);
            session.setSummarizedUpTo(0);
            session.setMessages(new ArrayList<>());
        }

        // 提取并保存 SystemMessage
        String systemMsg = null;
        for (ChatMessage msg : list) {
            if (msg instanceof SystemMessage sm) {
                systemMsg = sm.text();
                break;
            }
        }

        int oldCount = lastReturnedCount.getOrDefault(memoryId, 0);
        List<ChatMessage> newMessages = list.subList(
                Math.min(oldCount, list.size()), list.size());

        // 转换新消息
        int currentRound = session.getCurrentRound();
        List<SessionMessage> toAppend = new ArrayList<>();

        for (ChatMessage msg : newMessages) {
            if (msg instanceof SystemMessage) {
                continue; // systemMessage 单独存，不进 messages 数组
            } else if (msg instanceof UserMessage) {
                currentRound++;
                toAppend.add(fromUserMessage((UserMessage) msg, currentRound));
            } else if (msg instanceof AiMessage aiMsg) {
                if (aiMsg.hasToolExecutionRequests()) {
                    for (ToolExecutionRequest req : aiMsg.toolExecutionRequests()) {
                        toAppend.add(SessionMessage.builder()
                                .round(currentRound)
                                .role("assistant")
                                .type("tool_call")
                                .content(req.arguments())
                                .toolCallId(req.id())
                                .toolName(req.name())
                                .summarized(false)
                                .build());
                    }
                } else {
                    toAppend.add(SessionMessage.builder()
                            .round(currentRound)
                            .role("assistant")
                            .type("final")
                            .content(aiMsg.text())
                            .summarized(false)
                            .build());
                }
            } else if (msg instanceof ToolExecutionResultMessage toolMsg) {
                toAppend.add(SessionMessage.builder()
                        .round(currentRound)
                        .role("tool")
                        .type("tool_result")
                        .content(toolMsg.text())
                        .toolCallId(toolMsg.id())
                        .toolName(toolMsg.toolName())
                        .summarized(false)
                        .build());
            }
        }

        if (isNew) {
            session.setCurrentRound(currentRound);
            session.setSystemMessage(systemMsg);
            session.setMessages(toAppend);
            mongoTemplate.save(session);
        } else {
            Query query = Query.query(Criteria.where("sessionId").is(memoryId.toString()));
            Update update = new Update()
                    .set("currentRound", currentRound);
            if (systemMsg != null) {
                update.set("systemMessage", systemMsg);
            }
            if (!toAppend.isEmpty()) {
                update.push("messages").each(toAppend.toArray());
            }
            mongoTemplate.updateFirst(query, update, ChatSession.class);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Query query = Query.query(Criteria.where("sessionId").is(memoryId.toString()));
        mongoTemplate.remove(query, ChatSession.class);
    }

    // ========== 内部方法 ==========

    private ChatSession findSession(Object memoryId) {
        Query query = Query.query(Criteria.where("sessionId").is(memoryId.toString()));
        return mongoTemplate.findOne(query, ChatSession.class);
    }

    private SessionMessage fromUserMessage(UserMessage msg, int round) {
        return SessionMessage.builder()
                .round(round)
                .role("user")
                .type("message")
                .content(msg.singleText())
                .summarized(false)
                .build();
    }

    private ChatMessage toLangChainMessage(SessionMessage msg) {
        return switch (msg.getRole()) {
            case "user" -> UserMessage.from(msg.getContent());
            case "assistant" -> {
                if ("tool_call".equals(msg.getType())) {
                    yield AiMessage.from(ToolExecutionRequest.builder()
                            .id(msg.getToolCallId())
                            .name(msg.getToolName())
                            .arguments(msg.getContent())
                            .build());
                }
                yield AiMessage.from(msg.getContent());
            }
            case "tool" -> ToolExecutionResultMessage.from(
                    msg.getToolCallId(),
                    msg.getToolName(),
                    msg.getContent());
            default -> throw new IllegalArgumentException("Unknown role: " + msg.getRole());
        };
    }
}