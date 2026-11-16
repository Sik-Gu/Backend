package Sikgu.com.example.Web.domain.auth.dto.response;

/**
 * 인증·회원가입 API 응답 DTO 모음.
 */
public final class AuthResponses {

    /** POST /api/auth/email/send */
    public record EmailSendResponse(String email, long expiresInSec) {
    }

    /** POST /api/auth/email/verify */
    public record EmailVerifyResponse(String verificationToken, long expiresInSec) {
    }

    /** POST /api/auth/signup */
    public record SignupResponse(Long userId, String nickname) {
    }

    /** POST /api/auth/login */
    public record LoginResponse(String accessToken, String refreshToken, Long userId, String nickname) {
    }

    /** POST /api/auth/reissue */
    public record TokenResponse(String accessToken, String refreshToken) {
    }

    private AuthResponses() {
    }
}
