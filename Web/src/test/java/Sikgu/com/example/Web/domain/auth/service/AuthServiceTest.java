package Sikgu.com.example.Web.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.auth.dto.request.SignupRequest;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.CheckIdResponse;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.SignupResponse;
import Sikgu.com.example.Web.domain.point.entity.PointWallet;
import Sikgu.com.example.Web.domain.point.repository.PointWalletRepository;
import Sikgu.com.example.Web.domain.user.dto.request.LocationRequest;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.enums.UserRole;
import Sikgu.com.example.Web.domain.user.enums.UserStatus;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.domain.user.util.NicknameGenerator;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PointWalletRepository pointWalletRepository;
    @Mock
    EmailVerificationService emailVerificationService;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    NicknameGenerator nicknameGenerator;

    @InjectMocks
    AuthService authService;

    private final SignupRequest request = new SignupRequest(
            "token-1", "haeseung01", "pass123!",
            new LocationRequest("역삼동", 37.5006, 127.0364));

    @Test
    void 회원가입하면_해시된_비밀번호로_사용자와_잔액0_지갑을_만들고_토큰을_지운다() {
        given(emailVerificationService.getVerifiedEmail("token-1")).willReturn("user@korea.ac.kr");
        given(passwordEncoder.encode("pass123!")).willReturn("$2a$10$hashed");
        given(nicknameGenerator.generate()).willReturn("배고픈 고구마");

        SignupResponse response = authService.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getLoginId()).isEqualTo("haeseung01");
        assertThat(saved.getSchoolEmail()).isEqualTo("user@korea.ac.kr");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(saved.getDongName()).isEqualTo("역삼동");
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);

        ArgumentCaptor<PointWallet> walletCaptor = ArgumentCaptor.forClass(PointWallet.class);
        verify(pointWalletRepository).save(walletCaptor.capture());
        assertThat(walletCaptor.getValue().getAvailableBalance()).isZero();
        assertThat(walletCaptor.getValue().getUser()).isSameAs(saved);

        verify(emailVerificationService).consumeToken("token-1");
        assertThat(response.nickname()).isEqualTo("배고픈 고구마");
    }

    @Test
    void 아이디가_중복이면_USER409() {
        given(emailVerificationService.getVerifiedEmail("token-1")).willReturn("user@korea.ac.kr");
        given(userRepository.existsByLoginId("haeseung01")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.DUPLICATE_LOGIN_ID);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void 이메일이_이미_가입됐으면_USER409() {
        given(emailVerificationService.getVerifiedEmail("token-1")).willReturn("user@korea.ac.kr");
        given(userRepository.existsBySchoolEmail("user@korea.ac.kr")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.DUPLICATE_EMAIL);
    }

    @Test
    void 동시_가입으로_UNIQUE_제약에_걸리면_USER409이고_토큰은_남긴다() {
        given(emailVerificationService.getVerifiedEmail("token-1")).willReturn("user@korea.ac.kr");
        given(userRepository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.DUPLICATE_USER);
        verify(pointWalletRepository, never()).save(any());
        verify(emailVerificationService, never()).consumeToken(any());
    }

    @Test
    void 인증_토큰이_무효면_가입하지_않는다() {
        given(emailVerificationService.getVerifiedEmail("token-1"))
                .willThrow(new GeneralException(ErrorStatus.VERIFICATION_TOKEN_INVALID));

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.VERIFICATION_TOKEN_INVALID);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void 아이디가_없으면_사용_가능() {
        given(userRepository.existsByLoginId("newid")).willReturn(false);

        CheckIdResponse response = authService.checkLoginId("newid");

        assertThat(response.available()).isTrue();
    }
}
