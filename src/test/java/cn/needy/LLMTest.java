package cn.needy;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-04 12:10
 **/

@SpringBootTest
public class LLMTest {
    @Test
    public void testGptDemo(){
        OpenAiChatModel demo = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();

        String response = demo.chat("你是什么模型");
        System.out.println(response);
    }

    @Autowired
    private OpenAiChatModel model;
    @Test
    public void testSpringBoot(){
        String response = model.chat("你是什么模型");
        System.out.println(response);
    }
}
