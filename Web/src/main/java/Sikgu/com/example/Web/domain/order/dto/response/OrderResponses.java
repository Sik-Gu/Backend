package Sikgu.com.example.Web.domain.order.dto.response;

import Sikgu.com.example.Web.domain.participation.enums.ParticipationRole;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문·배송·나눔 API 응답 DTO 모음 (docs/API.md 7장).
 */
public final class OrderResponses {

    /** 차액 정산 방향 (D9) */
    public enum AdjustmentType {
        /** 영수증 금액 = 모집 금액 */
        NONE,
        /** 영수증이 더 쌈 → 환급 */
        REFUND,
        /** 영수증이 더 비쌈 → 추가 청구 */
        CHARGE
    }

    /** POST /api/posts/{postId}/order */
    public record OrderRegisterResponse(
            Long orderId,
            PostStatus postStatus,
            int receiptAmount,
            int recruitedAmount,
            boolean amountMatched,
            int difference,
            AdjustmentType adjustmentType,
            List<Adjustment> adjustments
    ) {
    }

    /**
     * @param amount        청구(+)·환급(-) 금액
     * @param chargedAmount 실제로 지갑에서 빠진 금액
     * @param debtAmount    잔액 부족으로 미수금이 된 금액
     */
    public record Adjustment(Long userId, ParticipationRole role, int units, long amount, long chargedAmount,
                             long debtAmount) {
    }

    /** GET /api/posts/{postId}/order */
    public record OrderDetailResponse(
            Long orderId,
            String courier,
            String trackingNumber,
            int receiptAmount,
            int recruitedAmount,
            boolean amountMatched,
            int difference,
            AdjustmentType adjustmentType,
            MyAdjustment myAdjustment,
            List<String> receiptImageUrls,
            LocalDateTime registeredAt,
            LocalDateTime distributeAt,
            String distributePlace,
            LocalDateTime autoConfirmAt,
            long heldPoint
    ) {
    }

    public record MyAdjustment(long amount, long chargedAmount, long debtAmount) {
    }

    /** PATCH /api/posts/{postId}/order/shipping */
    public record ShippingResponse(PostStatus postStatus, LocalDateTime shippedAt) {
    }

    /** PATCH /api/posts/{postId}/order/distribution */
    public record DistributionResponse(PostStatus postStatus, LocalDateTime distributedAt,
                                       LocalDateTime autoConfirmAt) {
    }

    private OrderResponses() {
    }
}
