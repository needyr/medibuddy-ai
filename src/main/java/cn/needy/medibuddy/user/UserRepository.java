package cn.needy.medibuddy.user;

import java.util.Optional;

public interface UserRepository {
    // 按用户名查询用户
    Optional<UserAccount> findByUsername(String username);
    // 保存/更新用户
    UserAccount save(UserAccount userAccount);
}
