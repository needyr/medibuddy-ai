package cn.needy.medibuddy;

import cn.needy.medibuddy.ai.assistant.Assistant;
import cn.needy.medibuddy.ai.assistant.MemoryChatAssistant;
import cn.needy.medibuddy.ai.assistant.SeparateChatAssistant;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-04 12:59
 **/

@SpringBootTest
public class ChatMemoryTest {
    @Autowired
    private Assistant assistant;
    @Autowired
    private OpenAiChatModel model;
    @Test
    public void testChatMemory(){
        String res = assistant.chat("我是nessy4431");
        System.out.println( res);

        String res2 = assistant.chat("我是谁");
        System.out.println( res2);
    }

    @Test
    public void testChatMemory2(){
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);

        AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .chatMemory(memory)
                .build();

        String res = assistant.chat("我是nessy4431");
        System.out.println( res);

        String res2 = assistant.chat("我是谁");
        System.out.println( res2);
    }

    @Autowired
    private MemoryChatAssistant memoryChatAssistant;
    @Test
    public void testChatMemory3(){
        String res = memoryChatAssistant.chat("我是nessy4431");
        System.out.println( res);

        String res2 = memoryChatAssistant.chat("我是谁");
        System.out.println( res2);
    }

    @Autowired
    private SeparateChatAssistant separateChatAssistant;
    @Test
    public void testChatMemory4(){
        // // 首次与 memoryId=1 的用户对话
        // String res = separateChatAssistant.chat(1,"我是糖糖");
        // System.out.println("第一次：" + res);
        //
        // // 再次与 memoryId=1 的用户对话，应该能记住之前的信息
        // String res2 = separateChatAssistant.chat(1,"我是谁");
        // System.out.println("第二次：" + res2);
        //
        // // 与 memoryId=2 的新用户对话，由于没有历史记录，会返回不同的回答
        // String res3 = separateChatAssistant.chat(2,"我是谁");
        // System.out.println("第三次（新用户）：" + res3);

        String res4 = separateChatAssistant.chat(3,"今天几号？");
        System.out.println("第四次（新用户）：" + res4);
    }
}
