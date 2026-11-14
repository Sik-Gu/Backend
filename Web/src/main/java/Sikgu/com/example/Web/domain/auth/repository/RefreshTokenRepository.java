package Sikgu.com.example.Web.domain.auth.repository;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Refresh Token 저장소 (Redis). 사용자당 1개만 저장한다.
 * 다른 기기에서 로그인하면 이전 기기의 Refresh Token은 무효가 된다.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, refreshToken, ttl);
    }

    public Optional<String> findByUserId(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + userId));
    }

    public void deleteByUserId(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
