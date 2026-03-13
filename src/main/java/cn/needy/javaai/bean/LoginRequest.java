package cn.needy.javaai.bean;

import lombok.Data;

@Data
public class LoginRequest {
    // 用户名
    private String username;
    // 明文密码（仅用于登录请求，不落库）
    private String password;
}
