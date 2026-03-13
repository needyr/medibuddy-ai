package cn.needy.javaai.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserAccount {
    // 用户唯一 ID
    private Long id;
    // 用户名
    private String username;
    // BCrypt 密码哈希
    private String passwordHash;
}
