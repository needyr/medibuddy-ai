package cn.needy;

import cn.needy.javaai.assistant.SeparateChatAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-05 10:14
 **/

@SpringBootTest
public class ToolTest {
    @Autowired
    private SeparateChatAssistant separateChatAssistant;

    @Test
    public void testCalculator() {
        String result = separateChatAssistant.chat(88, "请计算1+1,以及595588877的开平方是多少？");
        System.out.println(result);
    }
}
