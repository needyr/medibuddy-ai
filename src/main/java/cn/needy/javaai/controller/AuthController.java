package cn.needy.javaai.controller;

import cn.needy.javaai.bean.LoginRequest;
import cn.needy.javaai.bean.LoginResponse;
import cn.needy.javaai.common.Result;
import cn.needy.javaai.security.JwtService;
import cn.needy.javaai.user.UserAccount;
import cn.needy.javaai.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    // 登录签发 JWT（无状态鉴权）
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return Result.fail(400, "username and password are required");
        }
        try {
            UserAccount user = userService.authenticate(request.getUsername(), request.getPassword());
            String token = jwtService.generateToken(user);
            long expiresAt = Instant.now()
                    .plus(jwtService.getExpirationDays(), ChronoUnit.DAYS)
                    .toEpochMilli();
            LoginResponse response = new LoginResponse(user.getId(), user.getUsername(), token, expiresAt);
            return Result.success(response);
        } catch (IllegalArgumentException ex) {
            return Result.fail(401, "Invalid username or password");
        }
    }
}
