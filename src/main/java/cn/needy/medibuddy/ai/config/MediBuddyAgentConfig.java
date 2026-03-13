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
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@RequiredArgsConstructor
public class MediBuddyAgentConfig {

    private final MongoChatMemoryStore mongoChatMemoryStore;
    private final EmbeddingModel embeddingModel;
    private final RedisEmbeddingStore store;
    @Bean
    public ChatMemoryProvider mediBuddyChatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(30)
                .chatMemoryStore(mongoChatMemoryStore)
                .build();
    }

    @Bean
    public ContentRetriever mediBuddyContentRetriever() {
        ContentRetriever delegate = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .maxResults(1)
                .minScore(0.8)
                .build();

        // return query -> {
        //     System.out.println("开始执行 RAG 检索，query = " + query.text());
        //     var contents = delegate.retrieve(query);
        //     System.out.println("RAG 检索结果数量 = " + contents.size());
        //     System.out.println("RAG 检索结果 = " + contents);
        //     return contents;
        // };
        return delegate;
    }

}


