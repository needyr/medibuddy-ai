package cn.needy.medibuddy.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_sessions")
@CompoundIndex(name = "idx_session", def = "{'sessionId': 1}")
public class ChatSession {
    @Id
    private String id;
    private String sessionId;   // 对应 LangChain4j 的 memoryId
    private String userId;
    private String summary;     // 摘要文本，初始为 null
    private String systemMessage;
    private int summarizedUpTo; // 已摘要到哪个 round
    private int currentRound;   // 当前最新 round
    private List<SessionMessage> messages = new ArrayList<>();
}