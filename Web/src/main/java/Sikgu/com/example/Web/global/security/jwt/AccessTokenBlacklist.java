package Sikgu.com.example.Web.global.security.jwt;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 로그아웃한 Access Token 차단 목록 (Redis).
 * JWT는 만료 전까지 스스로 무효가 되지 않으므로, 로그아웃하면 남은 유효시간 동안 토큰 ID(jti)를 막아둔다.
 */
@Component
@RequiredArgsConstructor
public class AccessTokenBlacklist {

    static final String KEY_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void add(String tokenId, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            return; // 이미 만료된 토큰은 막을 필요 없음
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, "logout", ttl);
    }

    public boolean contains(String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tokenId));
    }
}
