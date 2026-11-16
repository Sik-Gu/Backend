package Sikgu.com.example.Web.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import Sikgu.com.example.Web.global.security.jwt.JwtProvider.JwtClaims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-for-sikgu-jwt-must-be-32-bytes-or-more";
    private static final Instant NOW = Instant.parse("2026-11-15T10:00:00Z");

    private final JwtProperties properties =
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14));
    private final JwtProvider provider = providerAt(NOW);

    @Test
    void Access_Token을_발급하고_내용을_꺼낼_수_있다() {
        String token = provider.createAccessToken(12L, "USER");

        JwtClaims claims = provider.parse(token);

        assertThat(claims.userId()).isEqualTo(12L);
        assertThat(claims.role()).isEqualTo("USER");
        assertThat(claims.accessToken()).isTrue();
        assertThat(claims.tokenId()).isNotBlank();
        assertThat(claims.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
    }

    @Test
    void Refresh_Token은_access가_아니고_14일_유효하다() {
        JwtClaims claims = provider.parse(provider.createRefreshToken(12L, "USER"));

        assertThat(claims.accessToken()).isFalse();
        assertThat(claims.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(14)));
    }

    @Test
    void 토큰마다_고유_ID가_다르다() {
        String a = provider.createAccessToken(12L, "USER");
        String b = provider.createAccessToken(12L, "USER");

        assertThat(provider.parse(a).tokenId()).isNotEqualTo(provider.parse(b).tokenId());
    }

    @Test
    void 만료된_토큰은_검증에_실패한다() {
        String token = provider.createAccessToken(12L, "USER");
        JwtProvider later = providerAt(NOW.plus(Duration.ofMinutes(31)));

        assertThatThrownBy(() -> later.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void 다른_키로_서명한_토큰은_검증에_실패한다() {
        JwtProvider other = new JwtProvider(
                new JwtProperties("another-secret-key-for-sikgu-jwt-32-bytes-long!!", Duration.ofMinutes(30),
                        Duration.ofDays(14)),
                Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        String forged = other.createAccessToken(12L, "ADMIN");

        assertThatThrownBy(() -> provider.parse(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void 남은_유효시간을_계산한다() {
        JwtClaims claims = provider.parse(provider.createAccessToken(12L, "USER"));
        JwtProvider tenMinutesLater = providerAt(NOW.plus(Duration.ofMinutes(10)));

        assertThat(tenMinutesLater.remainingValidity(claims)).isEqualTo(Duration.ofMinutes(20));
    }

    @Test
    void 서명_키가_없으면_시작하지_않는다() {
        JwtProperties empty = new JwtProperties(" ", Duration.ofMinutes(30), Duration.ofDays(14));

        assertThatThrownBy(() -> new JwtProvider(empty, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 서명_키가_32바이트보다_짧으면_시작하지_않는다() {
        JwtProperties weak = new JwtProperties("short-key", Duration.ofMinutes(30), Duration.ofDays(14));

        assertThatThrownBy(() -> new JwtProvider(weak, Clock.systemUTC()))
                .isInstanceOf(WeakKeyException.class);
    }

    private JwtProvider providerAt(Instant instant) {
        return new JwtProvider(properties, Clock.fixed(instant, ZoneId.of("Asia/Seoul")));
    }
}
