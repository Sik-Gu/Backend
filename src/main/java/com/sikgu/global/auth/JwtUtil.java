package com.sikgu.global.auth;

import com.sikgu.global.apiPayload.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final Duration accessExpiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") Long accessExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = Duration.ofSeconds(accessExpiration);
    }

    public String createAccessToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessExpiration)))
                .signWith(secretKey)
                .compact();
    }

    public Long getUserId(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(token);
            return Long.valueOf(claims.getPayload().getSubject());
        } catch (ExpiredJwtException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH401_4",
                    "토큰이 만료되었습니다. 다시 로그인해주세요.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH401_3",
                    "유효하지 않은 토큰입니다.");
        }
    }
}
