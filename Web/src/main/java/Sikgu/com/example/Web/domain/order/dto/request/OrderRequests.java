package Sikgu.com.example.Web.domain.order.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문·배송·나눔 API 요청 DTO 모음 (docs/API.md 7장).
 */
public final class OrderRequests {

    /**
     * POST /api/posts/{postId}/order. 영수증 사진만 받는다.
     * 할인·배달비 등으로 실제 결제 금액이 게시글 가격과 달라도 차액 정산은 하지 않는다 (게시글 가격 기준).
     */
    public record OrderRegisterRequest(
            @NotEmpty(message = "영수증 사진은 1장 이상 필요합니다.")
            @Size(max = 5, message = "영수증 사진은 5장까지 넣을 수 있습니다.")
            List<@NotBlank(message = "이미지 URL이 비었습니다.") @Size(max = 500) String> receiptImageUrls
    ) {
    }

    /** PATCH /api/posts/{postId}/order/shipping. 오프라인 구매면 둘 다 비운다 */
    public record ShippingRequest(
            @Size(max = 30, message = "택배사는 30자 이하입니다.")
            String courier,

            @Size(max = 30, message = "송장번호는 30자 이하입니다.")
            @Pattern(regexp = "^[0-9-]*$", message = "송장번호는 숫자와 하이픈만 쓸 수 있습니다.")
            String trackingNumber
    ) {
        /** 택배사와 송장번호는 둘 다 보내거나 둘 다 비운다 */
        @AssertTrue(message = "택배사와 송장번호는 함께 입력해야 합니다.")
        public boolean isBothOrNone() {
            return isBlank(courier) == isBlank(trackingNumber);
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }

    /** PATCH /api/posts/{postId}/order/distribution (D6: 나눔 일시 입력. 장소는 주문 전에 합의한 곳) */
    public record DistributionRequest(
            @NotNull(message = "나눔 일시는 필수입니다.")
            LocalDateTime distributeAt
    ) {
    }

    private OrderRequests() {
    }
}
