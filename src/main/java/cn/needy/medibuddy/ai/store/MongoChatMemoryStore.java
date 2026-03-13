package cn.needy.medibuddy.store;

import cn.needy.medibuddy.bean.ChatMessages;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-04 14:05
 **/

@Component
@RequiredArgsConstructor
public class MongoChatMemoryStore implements ChatMemoryStore {
    private final MongoTemplate mongoTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId.toString());
        Query query = new Query(criteria);
        ChatMessages one = mongoTemplate.findOne(query, ChatMessages.class);
        if (one == null) {
            return new LinkedList<>();
        }
        String content = one.getContent();
        if (content == null || content.trim().isEmpty()) {
            return new LinkedList<>();
        }
        return ChatMessageDeserializer.messagesFromJson(content);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        
        Criteria criteria = Criteria.where("memoryId").is(memoryId.toString());
        Query query = new Query(criteria);
        
        ChatMessages chatMessages = new ChatMessages();
        chatMessages.setMemoryId(memoryId.toString());
        chatMessages.setContent(ChatMessageSerializer.messagesToJson(list));
        
        mongoTemplate.upsert(query, new Update().set("content", chatMessages.getContent()), ChatMessages.class);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);
    }
}
