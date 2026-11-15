package Sikgu.com.example.Web.domain.review.dto.request;

import Sikgu.com.example.Web.domain.review.enums.ReviewTag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** POST /api/posts/{postId}/reviews (여러 명에게 한 번에) */
public record ReviewWriteRequest(
        @NotEmpty(message = "후기를 1개 이상 보내 주세요.")
        @Valid
        List<ReviewItem> reviews
) {

    public record ReviewItem(
            @NotNull(message = "평가 대상은 필수입니다.")
            Long targetUserId,

            @NotNull(message = "별점은 필수입니다.")
            @Min(value = 1, message = "별점은 1~5입니다.")
            @Max(value = 5, message = "별점은 1~5입니다.")
            Integer rating,

            @Size(max = 4, message = "태그는 4개까지 고를 수 있습니다.")
            List<@NotNull ReviewTag> tags,

            @Size(max = 500, message = "후기는 500자 이하입니다.")
            String content
    ) {
    }
}
