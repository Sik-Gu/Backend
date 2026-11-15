package com.sikgu.web.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;

    public JwtProvider(JwtProperties jwtProperties) {
        // yaml의 문자열 비밀키를 서명용 키 객체로 바꿈
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
        this.accessTokenExpiration = jwtProperties.accessTokenExpiration();
    }

    // 로그인 성공 시 토큰 발급
    public String createAccessToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // 토큰에서 userId 꺼내기
    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    // 토큰이 유효한지 (서명 맞는지, 만료 안 됐는지)
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}