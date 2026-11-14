package Sikgu.com.example.Web.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.EmailSendResponse;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.EmailVerifyResponse;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.global.infra.mail.EmailSender;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailVerificationServiceTest {

    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOps;
    @Mock
    UserRepository userRepository;
    @Mock
    EmailSender emailSender;

    @InjectMocks
    EmailVerificationService service;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
    }

    @Test
    void 학교_이메일이면_6자리_코드를_5분_저장하고_메일을_보낸다() {
        given(userRepository.existsBySchoolEmail("user@korea.ac.kr")).willReturn(false);

        EmailSendResponse response = service.sendCode("  User@Korea.AC.KR ");

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(eq("email:code:user@korea.ac.kr"), code.capture(), eq(Duration.ofMinutes(5)));
        assertThat(code.getValue()).matches("\\d{6}");
        verify(emailSender).send(eq("user@korea.ac.kr"), anyString(), anyString());
        assertThat(response.email()).isEqualTo("user@korea.ac.kr");
        assertThat(response.expiresInSec()).isEqualTo(300);
    }

    @Test
    void 학교_이메일이_아니면_AUTH4001() {
        assertThatThrownBy(() -> service.sendCode("user@gmail.com"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.NOT_SCHOOL_EMAIL);
        verify(emailSender, never()).send(any(), any(), any());
    }

    @Test
    void 이미_가입된_이메일이면_USER409() {
        given(userRepository.existsBySchoolEmail("user@korea.ac.kr")).willReturn(true);

        assertThatThrownBy(() -> service.sendCode("user@korea.ac.kr"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.DUPLICATE_EMAIL);
    }

    @Test
    void 코드가_맞으면_코드를_지우고_토큰을_30분_발급한다() {
        given(valueOps.get("email:code:user@korea.ac.kr")).willReturn("123456");

        EmailVerifyResponse response = service.verifyCode("user@korea.ac.kr", "123456");

        verify(redisTemplate).delete("email:code:user@korea.ac.kr");
        verify(valueOps).set("email:verified:" + response.verificationToken(), "user@korea.ac.kr",
                Duration.ofMinutes(30));
        assertThat(response.expiresInSec()).isEqualTo(1800);
    }

    @Test
    void 코드가_다르면_AUTH4002() {
        given(valueOps.get("email:code:user@korea.ac.kr")).willReturn("123456");

        assertThatThrownBy(() -> service.verifyCode("user@korea.ac.kr", "654321"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.VERIFICATION_CODE_MISMATCH);
    }

    @Test
    void 코드가_없거나_만료면_AUTH4003() {
        given(valueOps.get("email:code:user@korea.ac.kr")).willReturn(null);

        assertThatThrownBy(() -> service.verifyCode("user@korea.ac.kr", "123456"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.VERIFICATION_CODE_EXPIRED);
    }

    @Test
    void 토큰이_없거나_만료면_AUTH4004() {
        given(valueOps.get("email:verified:bad-token")).willReturn(null);

        assertThatThrownBy(() -> service.getVerifiedEmail("bad-token"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.VERIFICATION_TOKEN_INVALID);
    }
}
