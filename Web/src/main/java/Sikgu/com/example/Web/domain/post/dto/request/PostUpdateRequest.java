package Sikgu.com.example.Web.domain.post.dto.request;

import Sikgu.com.example.Web.domain.post.enums.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PATCH /api/posts/{postId}. 보낸 필드(null이 아닌 값)만 바뀐다.
 * 가격·단위 관련 필드(totalUnits, totalPrice, hostUnits, baseUnit)는 없다: 보내도 무시된다 (D8).
 */
public record PostUpdateRequest(
        @Size(max = 50, message = "제목은 50자 이하입니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "제목은 비울 수 없습니다.")
        String title,

        @Size(max = 100, message = "상품명은 100자 이하입니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "상품명은 비울 수 없습니다.")
        String productName,

        @Size(max = 500, message = "상품 링크는 500자 이하입니다.")
        String productLink,

        Category category,

        @Size(max = 2000, message = "내용은 2,000자 이하입니다.")
        String content,

        /** 보내면 이미지 목록 전체를 이 값으로 바꾼다 */
        @Size(max = 10, message = "이미지는 10개까지 넣을 수 있습니다.")
        List<@NotBlank(message = "이미지 URL이 비었습니다.") @Size(max = 500) String> imageUrls,

        LocalDateTime deadlineAt,

        /** 바꿀 때는 name·dongName·latitude·longitude를 모두 보낸다 */
        @Valid
        TradePlaceRequest tradePlace
) {
}
