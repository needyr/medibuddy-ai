package cn.needy.javaai.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    // 用户 ID
    private Long userId;
    // 用户名
    private String username;
    // JWT Token
    private String token;
    // 过期时间（毫秒时间戳）
    private Long expiresAt;
}
