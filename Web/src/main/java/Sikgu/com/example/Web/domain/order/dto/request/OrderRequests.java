package Sikgu.com.example.Web.domain.order.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
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

    /** POST /api/posts/{postId}/order */
    public record OrderRegisterRequest(
            @NotNull(message = "영수증 금액은 필수입니다.")
            @Min(value = 1, message = "영수증 금액은 1원 이상입니다.")
            Integer receiptAmount,

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

    /** PATCH /api/posts/{postId}/order/distribution (D6: 나눔 일정·장소 입력) */
    public record DistributionRequest(
            @NotNull(message = "나눔 일시는 필수입니다.")
            LocalDateTime distributeAt,

            @NotBlank(message = "나눔 장소는 필수입니다.")
            @Size(max = 100, message = "나눔 장소는 100자 이하입니다.")
            String distributePlace
    ) {
    }

    private OrderRequests() {
    }
}
