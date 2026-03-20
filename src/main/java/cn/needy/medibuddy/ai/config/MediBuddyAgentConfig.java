package cn.needy.medibuddy.ai.config;

import cn.needy.medibuddy.ai.store.MongoChatMemoryStore;
import dev.langchain4j.community.dashscope.spring.ChatModelProperties;
import dev.langchain4j.community.dashscope.spring.Properties;
import dev.langchain4j.community.model.dashscope.QwenTokenizer;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
        // return memoryId -> MessageWindowChatMemory.builder()
        //         .id(memoryId)
        //         .maxMessages(30)
        //         .chatMemoryStore(mongoChatMemoryStore)
        //         .build();
        return  memoryId -> TokenWindowChatMemory.builder()
                .id(memoryId)
                .maxTokens(10000, new OpenAiTokenizer(OpenAiChatModelName.GPT_4_O))
                .chatMemoryStore(mongoChatMemoryStore)
                .build();
    }

    // RAG 检索器：向量检索 + 置信度过滤
    @Bean
    public ContentRetriever mediBuddyContentRetriever() {
        System.out.println("使用了向量检索");
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .maxResults(2)
                .minScore(0.8)
                .build();
    }

    // 流式模型：优先使用 streaming 配置；如无则回退到 chat-model 配置
    @Bean
    @ConditionalOnMissingBean(name = "qwenStreamingChatModel")
    public QwenStreamingChatModel qwenStreamingChatModel(Properties properties) {
        ChatModelProperties streaming = properties.getStreamingChatModel();
        ChatModelProperties base = streaming != null ? streaming : properties.getChatModel();
        if (base == null) {
            throw new IllegalStateException("DashScope chat model properties are missing");
        }

        return QwenStreamingChatModel.builder()
                .baseUrl(base.getBaseUrl())
                .apiKey(base.getApiKey())
                .modelName(base.getModelName())
                .build();
    }

}
