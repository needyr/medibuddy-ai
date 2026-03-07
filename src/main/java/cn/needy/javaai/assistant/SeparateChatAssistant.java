package cn.needy.javaai.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = "calculatorTools"
)
public interface SeparateChatAssistant {
    // @SystemMessage("你是我的好朋友，请用四川话回答问题，今天是{{current_date}}")
    @SystemMessage(fromResource = "separate-assistant-template.txt")
    String chat(@MemoryId int memoryId, @UserMessage String userMessage);

    @UserMessage("你是我的好朋友，请在回答时适当加入表情符号。{{msg}}")
    String chat2(@MemoryId int memoryId, @V("msg") String userMessage);

    @SystemMessage(fromResource = "separate-assistant-template3.txt")
    String chat3(@MemoryId int memoryId, @UserMessage String userMessage, @V("username") String username, @V("age") int age);
}
