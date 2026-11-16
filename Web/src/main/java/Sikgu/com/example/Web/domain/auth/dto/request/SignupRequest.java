package Sikgu.com.example.Web.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 회원가입. 학교 이메일은 인증 단계에서 받은 verificationToken으로 확인하고, 비밀번호만 새로 받는다.
 * 닉네임은 서버가 랜덤으로 만든다. 집 위치는 가입 후 PATCH /api/users/me/location 으로 따로 설정한다.
 */
public record SignupRequest(
        @NotBlank(message = "이메일 인증 토큰은 필수입니다.")
        String verificationToken,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(regexp = AuthPatterns.PASSWORD, message = AuthPatterns.PASSWORD_MESSAGE)
        String password
) {
}
