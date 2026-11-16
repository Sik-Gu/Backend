package Sikgu.com.example.Web.domain.order.dto.response;

import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문·배송·나눔 API 응답 DTO 모음 (docs/API.md 7장).
 * 영수증 금액·차액 정산은 하지 않는다 (게시글 가격 기준).
 */
public final class OrderResponses {

    /** POST /api/posts/{postId}/order */
    public record OrderRegisterResponse(Long orderId, PostStatus postStatus, int totalPrice, String distributePlace) {
    }

    /** GET /api/posts/{postId}/order */
    public record OrderDetailResponse(
            Long orderId,
            String courier,
            String trackingNumber,
            int totalPrice,
            List<String> receiptImageUrls,
            LocalDateTime registeredAt,
            LocalDateTime distributeAt,
            String distributePlace,
            long heldPoint
    ) {
    }

    /** PATCH /api/posts/{postId}/order/shipping */
    public record ShippingResponse(PostStatus postStatus, LocalDateTime shippedAt) {
    }

    /** PATCH /api/posts/{postId}/order/distribution */
    public record DistributionResponse(PostStatus postStatus, LocalDateTime distributedAt, String distributePlace) {
    }

    private OrderResponses() {
    }
}
