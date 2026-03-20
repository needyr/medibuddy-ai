package cn.needy.medibuddy.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionMessage {
    private int round;
    private String role;        // user, assistant, tool
    private String type;        // message, tool_call, tool_result, final
    private String content;
    private boolean summarized;
    // tool 相关，普通消息为 null
    private String toolCallId;
    private String toolName;
}