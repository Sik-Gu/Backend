package Sikgu.com.example.Web.domain.post.dto.request;

import Sikgu.com.example.Web.domain.post.enums.Category;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * POST /api/posts. 검증 규칙(길이·범위)은 가정값 (docs/API.md 6장).
 * 모집 마감은 받지 않는다: 등록 시각 + 7일로 고정 (D24).
 * 위치는 받지 않는다: 호스트의 집 위치(GPS)를 글에 저장한다. 나눔 장소는 모집 중에 참여자와 정한다.
 */
public record PostCreateRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 50, message = "제목은 50자 이하입니다.")
        String title,

        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 100, message = "상품명은 100자 이하입니다.")
        String productName,

        @Size(max = 500, message = "상품 링크는 500자 이하입니다.")
        String productLink,

        @NotNull(message = "카테고리는 필수입니다.")
        Category category,

        @Size(max = 2000, message = "내용은 2,000자 이하입니다.")
        String content,

        @Size(max = 10, message = "이미지는 10개까지 넣을 수 있습니다.")
        List<@NotBlank(message = "이미지 URL이 비었습니다.") @Size(max = 500) String> imageUrls,

        @NotBlank(message = "기본 단위는 필수입니다.")
        @Size(max = 30, message = "기본 단위는 30자 이하입니다.")
        String baseUnit,

        @NotNull(message = "총 단위 수는 필수입니다.")
        @Min(value = 2, message = "총 단위 수는 2 이상입니다.")
        @Max(value = 100, message = "총 단위 수는 100 이하입니다.")
        Integer totalUnits,

        @NotNull(message = "총 가격은 필수입니다.")
        @Min(value = 1, message = "총 가격은 1원 이상입니다.")
        @Max(value = 10_000_000, message = "총 가격은 1,000만 원 이하입니다.")
        Integer totalPrice,

        @NotNull(message = "호스트 구매 단위는 필수입니다.")
        @Min(value = 1, message = "호스트 구매 단위는 1 이상입니다.")
        Integer hostUnits
) {
}
