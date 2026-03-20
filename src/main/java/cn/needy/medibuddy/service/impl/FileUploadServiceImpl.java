package cn.needy.medibuddy.service.impl;

import cn.needy.medibuddy.service.FileUploadService;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-08 01:06
 **/

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {
    private final RedisEmbeddingStore store;
    private final EmbeddingModel embeddingModel;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String INGEST_FILE_HASH_KEY_PREFIX = "medibuddy:ingest:filename:";
    @Override
    public String uploadFile(MultipartFile file) {
        TextDocumentParser parser = new TextDocumentParser();
        Document document;
        try {
            document = parser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 1. 去重：用文档内容的 MD5 比对
        String documentHash = md5(document.text());
        String redisKey = INGEST_FILE_HASH_KEY_PREFIX + file.getOriginalFilename();
        String storedHash = stringRedisTemplate.opsForValue().get(redisKey);

        if (documentHash.equals(storedHash)) {
            return "文件已存在";
        }

        // 2. 构建 ingestor，加上分块策略
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .documentSplitter(DocumentSplitters.recursive(500, 50))
                .build();

        ingestor.ingest(document);
        stringRedisTemplate.opsForValue().set(redisKey, documentHash);
        return "文件上传成功";
    }


    private String md5(String content) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digest = messageDigest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is not available", e);
        }
    }
}
