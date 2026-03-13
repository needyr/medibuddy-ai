package cn.needy.medibuddy;

import cn.needy.medibuddy.ai.assistant.Assistant;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-04 12:50
 **/

@SpringBootTest
public class AiServiceTest {
    @Autowired
    private OpenAiChatModel model;
    @Test
    public void testChat(){
        Assistant assistant = AiServices.create(Assistant.class, model);
        String res = assistant.chat("你是谁");
        System.out.println(res);

    }

    @Autowired
    private Assistant assistant;
    @Test
    public void testAssistant(){
        String res = assistant.chat("我是谁");
    }
}
