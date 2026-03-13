package cn.needy.javaai.bean;

import lombok.Data;

@Data
public class ChatForm {
    // 对话记忆 ID
    private Long memoryId;
    // 用户输入
    private String userMessage;
}
