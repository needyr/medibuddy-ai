package cn.needy.javaai.config;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: java-ai-langchain4j
 * @description: Redis向量存储
 * @author: yeguobingfen
 * @create: 2026-03-08 00:53
 **/

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
