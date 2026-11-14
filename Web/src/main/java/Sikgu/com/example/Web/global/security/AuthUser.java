package Sikgu.com.example.Web.global.security;

/**
 * 인증된 사용자 정보. 컨트롤러에서 {@code @AuthenticationPrincipal AuthUser authUser}로 받는다.
 */
public record AuthUser(Long userId, String role) {
}
