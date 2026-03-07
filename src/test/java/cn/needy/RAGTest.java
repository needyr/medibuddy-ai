package cn.needy;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-07 22:24
 **/

@SpringBootTest
public class RAGTest {

    @Test
    public void testReadDocument(){
        Document document = FileSystemDocumentLoader.loadDocument(
                "F:/work/java-ai-langchain4j/医院信息.md");
        System.out.println(document.text());
    }

    @Test
    public void testReadPdf(){
        Document document = FileSystemDocumentLoader.loadDocument(
                "F:/work/java-ai-langchain4j/医院信息.pdf",
                new ApachePdfBoxDocumentParser());
        System.out.println(document.text());
    }

    @Autowired
    private EmbeddingModel embeddingModel;
    @Test
    public void testReadDocumentAndStore(){
        Document document = FileSystemDocumentLoader.loadDocument(
                "F:/work/java-ai-langchain4j/医院信息.md");

        // 基于内存的向量存储
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

        // 文档分割器，默认使用递归分割器,将文档分为多个文本片段，每个片段不超过300个token，大概是450字
        // 并且有30个token的重叠部分保证连贯性
        // 文本向量化，使用内置的轻量模型对文本进行向量化
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build();

        ingestor.ingest(document);
        // 查看向量数据库的内容
        System.out.println(store);
    }

    @Test
    public void testReadDocumentAndStore2(){
        Document document = FileSystemDocumentLoader.loadDocument(
                "F:/work/java-ai-langchain4j/医院信息.md");

        RedisEmbeddingStore store = RedisEmbeddingStore.builder()
                .host("localhost")
                .port(6380)
                .dimension(1024)
                .indexName("medibuddy_embedding_index")
                .prefix("medibuddy:")
                .build();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build();

        ingestor.ingest(document);
        System.out.println(store);
    }

    @Test
    public void testSearchFromRedisStore() {
        RedisEmbeddingStore store = RedisEmbeddingStore.builder()
                .host("localhost")
                .port(6380)
                .dimension(1024)
                .indexName("medibuddy_embedding_index")
                .prefix("medibuddy:")
                .build();

        Embedding queryEmbedding = embeddingModel.embed("这家医院的咨询电话是多少？").content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(3)
                .build();

        EmbeddingSearchResult<TextSegment> result = store.search(request);
        List<EmbeddingMatch<TextSegment>> matches = result.matches();

        for (EmbeddingMatch<TextSegment> match : matches) {
            System.out.println("score = " + match.score());
            System.out.println("text = " + match.embedded().text());
            System.out.println("metadata = " + match.embedded().metadata());
            System.out.println("--------------------");
        }
    }
}
