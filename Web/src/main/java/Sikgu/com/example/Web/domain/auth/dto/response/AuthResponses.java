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

    /** GET /api/auth/check-id */
    public record CheckIdResponse(String loginId, boolean available) {
    }

    /** POST /api/auth/signup */
    public record SignupResponse(Long userId, String nickname) {
    }

    private AuthResponses() {
    }
}
