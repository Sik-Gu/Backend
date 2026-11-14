package Sikgu.com.example.Web.domain.user.dto.response;

/**
 * 회원 API 응답 DTO 모음.
 */
public final class UserResponses {

    /** PATCH /api/users/me/location */
    public record LocationResponse(String dongName, Double latitude, Double longitude) {
    }

    private UserResponses() {
    }
}
