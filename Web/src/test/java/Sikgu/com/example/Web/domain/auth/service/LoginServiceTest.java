package Sikgu.com.example.Web.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.auth.dto.request.LoginRequest;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.LoginResponse;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.TokenResponse;
import Sikgu.com.example.Web.domain.auth.repository.RefreshTokenRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.enums.UserStatus;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.global.security.jwt.AccessTokenBlacklist;
import Sikgu.com.example.Web.global.security.jwt.JwtProperties;
import Sikgu.com.example.Web.global.security.jwt.JwtProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    AccessTokenBlacklist accessTokenBlacklist;

    /** 실제 JWT를 만들어 흐름 전체를 확인한다 */
    private final JwtProvider jwtProvider = new JwtProvider(
            new JwtProperties("test-secret-key-for-sikgu-jwt-must-be-32-bytes-or-more",
                    Duration.ofMinutes(30), Duration.ofDays(14)),
            CLOCK);

    private LoginService loginService;
    private User user;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(userRepository, passwordEncoder, jwtProvider,
                refreshTokenRepository, accessTokenBlacklist, CLOCK);
        user = User.create("haeseung01", "user@korea.ac.kr", "$2a$10$hashed", "배고픈 고구마",
                "역삼동", 37.5, 127.0);
        ReflectionTestUtils.setField(user, "id", 12L);
    }

    @Test
    void 비밀번호가_맞으면_토큰을_발급하고_Refresh_Token을_14일_저장한다() {
        given(userRepository.findByLoginId("haeseung01")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pass123!", "$2a$10$hashed")).willReturn(true);

        LoginResponse response = loginService.login(new LoginRequest("haeseung01", "pass123!"));

        assertThat(jwtProvider.parse(response.accessToken()).userId()).isEqualTo(12L);
        assertThat(jwtProvider.parse(response.accessToken()).accessToken()).isTrue();
        assertThat(jwtProvider.parse(response.refreshToken()).accessToken()).isFalse();
        assertThat(response.nickname()).isEqualTo("배고픈 고구마");
        verify(refreshTokenRepository).save(12L, response.refreshToken(), Duration.ofDays(14));
    }

    @Test
    void 아이디가_없으면_AUTH4011() {
        given(userRepository.findByLoginId("nobody")).willReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(new LoginRequest("nobody", "pass123!")))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.LOGIN_FAILED);
    }

    @Test
    void 비밀번호가_틀리면_아이디가_없을_때와_같은_AUTH4011() {
        given(userRepository.findByLoginId("haeseung01")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "$2a$10$hashed")).willReturn(false);

        assertThatThrownBy(() -> loginService.login(new LoginRequest("haeseung01", "wrong")))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.LOGIN_FAILED);
        verify(refreshTokenRepository, never()).save(any(), any(), any());
    }

    @Test
    void 정지된_계정이면_AUTH4031() {
        suspend(user, null);
        given(userRepository.findByLoginId("haeseung01")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pass123!", "$2a$10$hashed")).willReturn(true);

        assertThatThrownBy(() -> loginService.login(new LoginRequest("haeseung01", "pass123!")))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.SUSPENDED_USER);
    }

    @Test
    void 정지_기간이_끝났으면_로그인된다() {
        suspend(user, LocalDateTime.now(CLOCK).minusDays(1));
        given(userRepository.findByLoginId("haeseung01")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pass123!", "$2a$10$hashed")).willReturn(true);

        assertThat(loginService.login(new LoginRequest("haeseung01", "pass123!")).accessToken()).isNotBlank();
    }

    @Test
    void 저장된_Refresh_Token이면_새_토큰을_발급하고_교체한다() {
        String refreshToken = jwtProvider.createRefreshToken(12L, "USER");
        given(refreshTokenRepository.findByUserId(12L)).willReturn(Optional.of(refreshToken));
        given(userRepository.findById(12L)).willReturn(Optional.of(user));

        TokenResponse response = loginService.reissue(refreshToken);

        assertThat(response.refreshToken()).isNotEqualTo(refreshToken);
        verify(refreshTokenRepository).save(12L, response.refreshToken(), Duration.ofDays(14));
    }

    @Test
    void 이미_재발급에_쓴_Refresh_Token이면_AUTH4012() {
        String oldToken = jwtProvider.createRefreshToken(12L, "USER");
        String currentToken = jwtProvider.createRefreshToken(12L, "USER");
        given(refreshTokenRepository.findByUserId(12L)).willReturn(Optional.of(currentToken));

        assertThatThrownBy(() -> loginService.reissue(oldToken))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_REFRESH_TOKEN);
    }

    @Test
    void 로그아웃했거나_저장된_토큰이_없으면_AUTH4012() {
        String refreshToken = jwtProvider.createRefreshToken(12L, "USER");
        given(refreshTokenRepository.findByUserId(12L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.reissue(refreshToken))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_REFRESH_TOKEN);
    }

    @Test
    void Access_Token으로는_재발급할_수_없다() {
        String accessToken = jwtProvider.createAccessToken(12L, "USER");

        assertThatThrownBy(() -> loginService.reissue(accessToken))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_REFRESH_TOKEN);
    }

    @Test
    void 위조된_Refresh_Token이면_AUTH4012() {
        assertThatThrownBy(() -> loginService.reissue("not-a-jwt"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_REFRESH_TOKEN);
    }

    @Test
    void 로그아웃하면_Refresh_Token을_지우고_Access_Token을_남은_시간만큼_차단한다() {
        String accessToken = jwtProvider.createAccessToken(12L, "USER");
        String tokenId = jwtProvider.parse(accessToken).tokenId();

        loginService.logout(12L, accessToken);

        verify(refreshTokenRepository).deleteByUserId(12L);
        verify(accessTokenBlacklist).add(eq(tokenId), eq(Duration.ofMinutes(30)));
    }

    private static void suspend(User user, LocalDateTime until) {
        ReflectionTestUtils.setField(user, "status", UserStatus.SUSPENDED);
        ReflectionTestUtils.setField(user, "suspendedUntil", until);
    }
}
