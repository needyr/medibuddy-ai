package cn.needy.medibuddy.config;

import cn.needy.medibuddy.store.MongoChatMemoryStore;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MediBuddyAgentConfig {

    private final MongoChatMemoryStore mongoChatMemoryStore;
    private final EmbeddingModel embeddingModel;
    private final RedisEmbeddingStore store;

    // 对话记忆提供器：基于 Mongo 持久化，窗口大小 30 条
    @Bean
    public ChatMemoryProvider mediBuddyChatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(30)
                .chatMemoryStore(mongoChatMemoryStore)
                .build();
    }

    // RAG 检索器：向量检索 + 置信度过滤
    @Bean
    public ContentRetriever mediBuddyContentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .maxResults(1)
                .minScore(0.8)
                .build();
    }

}
