package cn.needy.medibuddy.security;

import cn.needy.medibuddy.user.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@Slf4j
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secret;

    @Getter
    @Value("${security.jwt.expiration-days:7}")
    private long expirationDays;

    private Key key;

    @PostConstruct
    public void init() {
        // 允许使用明文或 base64 作为密钥，统一转为 HMAC key
        byte[] keyBytes = Decoders.BASE64.decode(base64Url(secret));
        if (keyBytes.length < 32) {
            log.warn("JWT secret is too short ({} bytes). Please set JWT_SECRET to at least 32 bytes.", keyBytes.length);
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserAccount user) {
        // 生成 JWT：包含用户ID与用户名
        Instant now = Instant.now();
        Instant exp = now.plus(expirationDays, ChronoUnit.DAYS);
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("uid", user.getId())
                .claim("uname", user.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long extractUserId(String token) {
        // 从 token 中解析 userId
        Claims claims = parseClaims(token);
        Object uid = claims.get("uid");
        if (uid instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(uid));
    }

    public String extractUsername(String token) {
        // 从 token 中解析用户名
        Claims claims = parseClaims(token);
        return claims.get("uname", String.class);
    }

    private Claims parseClaims(String token) {
        // 校验签名与过期时间
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String base64Url(String raw) {
        // 如果不是 base64 字符串，按明文转为 base64
        // JJWT expects base64-encoded key. If user provides plain text, encode it.
        if (raw.matches("^[A-Za-z0-9+/=]+$") && raw.length() % 4 == 0) {
            return raw;
        }
        return java.util.Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
