package Sikgu.com.example.Web.domain.auth.dto.request;

import Sikgu.com.example.Web.domain.user.dto.request.LocationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SignupRequest(
        @NotBlank(message = "이메일 인증 토큰은 필수입니다.")
        String verificationToken,

        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(regexp = AuthPatterns.LOGIN_ID, message = AuthPatterns.LOGIN_ID_MESSAGE)
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(regexp = AuthPatterns.PASSWORD, message = AuthPatterns.PASSWORD_MESSAGE)
        String password,

        @NotNull(message = "동네 정보는 필수입니다.")
        @Valid
        LocationRequest location
) {
}
