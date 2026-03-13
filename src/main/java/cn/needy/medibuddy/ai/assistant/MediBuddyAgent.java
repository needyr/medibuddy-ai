package cn.needy.medibuddy.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

// AI 服务接口：由 LangChain4j 生成代理实现
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",
        chatMemoryProvider = "mediBuddyChatMemoryProvider",
        tools = "appointmentTools",
        contentRetriever = "mediBuddyContentRetriever"
)
public interface MediBuddyAgent {
    // 系统提示词来自资源文件，用户消息通过参数传入
    @SystemMessage(fromResource = "medibuddy-prompt-template.txt")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
