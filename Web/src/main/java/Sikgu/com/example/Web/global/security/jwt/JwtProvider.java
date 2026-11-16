package Sikgu.com.example.Web.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT 발급·검증.
 * <p>토큰 내용: sub=userId, role, type(access/refresh), jti(토큰 고유 ID, 로그아웃 차단에 사용), iat, exp
 */
@Component
public class JwtProvider {

    static final String CLAIM_ROLE = "role";
    static final String CLAIM_TYPE = "type";
    static final String TYPE_ACCESS = "access";
    static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final JwtParser parser;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtProvider(JwtProperties properties, Clock clock) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("JWT 서명 키가 없습니다. 환경변수 JWT_SECRET을 설정하세요.");
        }
        // HS256은 32바이트 이상 키가 필요하다. 짧으면 WeakKeyException
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build();
        this.properties = properties;
        this.clock = clock;
    }

    public String createAccessToken(Long userId, String role) {
        return createToken(userId, role, TYPE_ACCESS, properties.accessTokenValidity());
    }

    public String createRefreshToken(Long userId, String role) {
        return createToken(userId, role, TYPE_REFRESH, properties.refreshTokenValidity());
    }

    public Duration refreshTokenValidity() {
        return properties.refreshTokenValidity();
    }

    /**
     * 서명·만료를 검증하고 내용을 꺼낸다.
     *
     * @throws JwtException 위조·만료·형식 오류
     */
    public JwtClaims parse(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        return new JwtClaims(
                Long.valueOf(claims.getSubject()),
                claims.get(CLAIM_ROLE, String.class),
                TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class)),
                claims.getId(),
                claims.getExpiration().toInstant());
    }

    /** 만료까지 남은 시간. 로그아웃 시 차단 목록 보관 기간으로 쓴다. */
    public Duration remainingValidity(JwtClaims claims) {
        Duration remaining = Duration.between(clock.instant(), claims.expiresAt());
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private String createToken(Long userId, String role, String type, Duration validity) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, type)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    /**
     * 검증된 토큰 내용.
     *
     * @param accessToken access 토큰이면 true, refresh 토큰이면 false
     * @param tokenId     jti
     */
    public record JwtClaims(Long userId, String role, boolean accessToken, String tokenId, Instant expiresAt) {
    }
}
