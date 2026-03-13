package cn.needy.medibuddy.user;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@RequiredArgsConstructor
public class InMemoryUserRepository implements UserRepository {

    private final PasswordEncoder passwordEncoder;
    // 内存用户表：username -> UserAccount
    private final ConcurrentHashMap<String, UserAccount> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Value("${app.user.default.username:admin}")
    private String defaultUsername;

    @Value("${app.user.default.password:admin123}")
    private String defaultPassword;

    @PostConstruct
    public void initDefaultUser() {
        // 启动时创建默认用户，便于本地演示登录
        if (users.containsKey(defaultUsername)) {
            return;
        }
        String hash = passwordEncoder.encode(defaultPassword);
        UserAccount user = new UserAccount(idGenerator.getAndIncrement(), defaultUsername, hash);
        users.put(defaultUsername, user);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        if (userAccount.getId() == null) {
            userAccount.setId(idGenerator.getAndIncrement());
        }
        users.put(userAccount.getUsername(), userAccount);
        return userAccount;
    }
}
