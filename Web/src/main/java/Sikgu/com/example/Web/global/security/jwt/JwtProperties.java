package Sikgu.com.example.Web.global.security.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT 설정.
 * <ul>
 *   <li>secret: 서명 키. 비밀값이라 커밋하지 않고 환경변수 {@code JWT_SECRET}으로 넣는다 (32바이트 이상)</li>
 *   <li>accessTokenValidity: Access Token 유효시간 (기본 30분)</li>
 *   <li>refreshTokenValidity: Refresh Token 유효시간 (기본 14일)</li>
 * </ul>
 */
@ConfigurationProperties("jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("30m") Duration accessTokenValidity,
        @DefaultValue("14d") Duration refreshTokenValidity
) {
}
