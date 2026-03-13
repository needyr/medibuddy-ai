package cn.needy.medibuddy.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",
        chatMemoryProvider = "mediBuddyChatMemoryProvider",
        tools = "appointmentTools",
        contentRetriever = "mediBuddyContentRetriever"
)
public interface MediBuddyAgent {
    @SystemMessage(fromResource = "medibuddy-prompt-template.txt")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);
}