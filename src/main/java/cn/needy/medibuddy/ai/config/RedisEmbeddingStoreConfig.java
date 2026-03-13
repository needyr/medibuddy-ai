package cn.needy.medibuddy.config;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Redis 向量存储配置
@Configuration
public class RedisEmbeddingStoreConfig {
    @Value("${langchain4j.community.redis.host}")
    private String host;

    @Value("${langchain4j.community.redis.port}")
    private Integer port;

    @Value("${langchain4j.community.redis.dimension}")
    private Integer dimension;

    @Value("${langchain4j.community.redis.index-name}")
    private String indexName;

    @Value("${langchain4j.community.redis.prefix}")
    private String prefix;

    // 创建 Redis 向量存储实例（供 RAG 检索使用）
    @Bean
    public RedisEmbeddingStore store() {
        return RedisEmbeddingStore.builder()
                .host(host)
                .port(port)
                .dimension(dimension)
                .indexName(indexName)
                .prefix(prefix)
                .build();
    }
}
