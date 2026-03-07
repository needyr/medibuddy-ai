package cn.needy.javaai.bean;

import lombok.Data;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-05 09:00
 **/

@Data
public class ChatForm {
    // 对话id
    private Long memoryId;
    // 用户输入
    private String userMessage;
}
