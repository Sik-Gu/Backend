package Sikgu.com.example.Web.domain.participation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** POST /api/posts/{postId}/participations */
public record ParticipationJoinRequest(
        @NotNull(message = "참여 단위는 필수입니다.")
        @Min(value = 1, message = "참여 단위는 1 이상입니다.")
        Integer units
) {
}
