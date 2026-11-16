package Sikgu.com.example.Web.domain.auth.service;

import Sikgu.com.example.Web.domain.auth.dto.request.LoginRequest;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.LoginResponse;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.TokenResponse;
import Sikgu.com.example.Web.domain.auth.repository.RefreshTokenRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.global.security.jwt.AccessTokenBlacklist;
import Sikgu.com.example.Web.global.security.jwt.JwtProvider;
import Sikgu.com.example.Web.global.security.jwt.JwtProvider.JwtClaims;
import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인·토큰 재발급·로그아웃 (docs/API.md 4장)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenBlacklist accessTokenBlacklist;
    private final Clock clock;

    /**
     * 학교 이메일·비밀번호 확인 후 토큰 발급.
     * 가입되지 않은 이메일과 비밀번호가 틀린 경우를 같은 에러로 응답해서, 가입된 이메일인지 알 수 없게 한다.
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findBySchoolEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new GeneralException(ErrorStatus.LOGIN_FAILED));

        // BCrypt는 저장된 해시에서 salt를 꺼내 다시 해시해서 비교한다 (D7)
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new GeneralException(ErrorStatus.LOGIN_FAILED);
        }
        validateNotSuspended(user);

        TokenResponse tokens = issueTokens(user);
        return new LoginResponse(tokens.accessToken(), tokens.refreshToken(), user.getId(), user.getNickname());
    }

    /**
     * Refresh Token으로 토큰 재발급.
     * Refresh Token도 새로 발급해서(rotation) 이전 Refresh Token은 다시 쓸 수 없게 한다.
     */
    public TokenResponse reissue(String refreshToken) {
        JwtClaims claims = parseRefreshToken(refreshToken);

        String savedToken = refreshTokenRepository.findByUserId(claims.userId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN));
        if (!savedToken.equals(refreshToken)) {
            // 이미 재발급에 쓴 토큰이거나 다른 기기에서 다시 로그인한 경우
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN));
        validateNotSuspended(user);

        return issueTokens(user);
    }

    /**
     * 로그아웃. Refresh Token을 지우고, 지금 쓴 Access Token은 남은 유효시간 동안 차단한다.
     */
    public void logout(Long userId, String accessToken) {
        refreshTokenRepository.deleteByUserId(userId);

        JwtClaims claims = jwtProvider.parse(accessToken); // 필터에서 이미 검증된 토큰
        accessTokenBlacklist.add(claims.tokenId(), jwtProvider.remainingValidity(claims));
    }

    private TokenResponse issueTokens(User user) {
        String role = user.getRole().name();
        String accessToken = jwtProvider.createAccessToken(user.getId(), role);
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), role);
        refreshTokenRepository.save(user.getId(), refreshToken, jwtProvider.refreshTokenValidity());
        return new TokenResponse(accessToken, refreshToken);
    }

    private JwtClaims parseRefreshToken(String refreshToken) {
        JwtClaims claims;
        try {
            claims = jwtProvider.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }
        if (claims.accessToken()) {
            // Access Token으로는 재발급할 수 없다
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }
        return claims;
    }

    private void validateNotSuspended(User user) {
        if (user.isSuspended(LocalDateTime.now(clock))) {
            throw new GeneralException(ErrorStatus.SUSPENDED_USER);
        }
    }
}
