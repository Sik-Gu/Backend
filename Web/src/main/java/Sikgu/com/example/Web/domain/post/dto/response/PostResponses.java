package Sikgu.com.example.Web.domain.post.dto.response;

import Sikgu.com.example.Web.domain.participation.enums.ParticipationRole;
import Sikgu.com.example.Web.domain.participation.enums.PaymentStatus;
import Sikgu.com.example.Web.domain.participation.enums.ReceiptStatus;
import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.post.enums.DistanceRange;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 공구 게시글 API 응답 DTO 모음 (docs/API.md 6장).
 */
public final class PostResponses {

    /** POST /api/posts */
    public record PostCreateResponse(Long postId, Long chatRoomId, int unitPrice, long paidPoint, long balanceAfter) {
    }

    /** PATCH /api/posts/{postId} */
    public record PostIdResponse(Long postId) {
    }

    /** DELETE /api/posts/{postId} */
    public record PostCancelResponse(Long postId, PostStatus postStatus, int refundedCount, long refundedPoint) {
    }

    /** 목록 카드 (GET /api/posts 의 content[] 한 개) */
    public record PostCard(
            Long postId,
            String title,
            String thumbnailUrl,
            Category category,
            int unitPrice,
            String baseUnit,
            int totalUnits,
            int filledUnits,
            int participantCount,
            String dongName,
            /** 내 집 ↔ 글 올린 사람 집 거리 구간. 좌표·정확한 거리는 내려주지 않는다 */
            DistanceRange distance,
            PostStatus status,
            int viewCount,
            LocalDateTime deadlineAt,
            LocalDateTime createdAt
    ) {
    }

    /** GET /api/posts/{postId} */
    public record PostDetailResponse(
            Long postId,
            String title,
            String productName,
            String productLink,
            Category category,
            String content,
            List<String> imageUrls,
            String baseUnit,
            int totalUnits,
            int filledUnits,
            int participantCount,
            int unitPrice,
            int totalPrice,
            PostStatus status,
            StatusTimeline statusTimeline,
            String dongName,
            DistanceRange distance,
            /** 나눔 장소 합의 현황. 호스트가 아직 제안하지 않았으면 null */
            DistributionPlaceInfo distributionPlace,
            LocalDateTime deadlineAt,
            int viewCount,
            LocalDateTime createdAt,
            HostSummary host,
            List<ParticipantSummary> participants,
            MyParticipation me,
            int maxSelectableUnits,
            Long chatRoomId
    ) {
    }

    public record StatusTimeline(
            LocalDateTime recruitedAt,
            LocalDateTime orderedAt,
            LocalDateTime shippedAt,
            LocalDateTime distributedAt,
            LocalDateTime settledAt,
            LocalDateTime cancelledAt
    ) {
    }

    public record HostSummary(Long userId, String nickname, Double rating, int tradeCount,
                              List<Category> mainCategories) {
    }

    /** placeAgreed: 나눔 장소에 동의했는지 (장소 제안 전이면 false) */
    public record ParticipantSummary(Long userId, String nickname, ParticipationRole role, int units,
                                     Double rating, int tradeCount, List<Category> mainCategories,
                                     boolean placeAgreed) {
    }

    /**
     * 나눔 장소 합의 현황. 공개 장소라 좌표를 보여준다 (집 위치와는 별개).
     *
     * @param myAgreed 내가 동의했는지. 참여자가 아니면 null
     */
    public record DistributionPlaceInfo(String name, Double latitude, Double longitude, LocalDateTime proposedAt,
                                        int agreedCount, int memberCount, boolean allAgreed, Boolean myAgreed) {
    }

    public record MyParticipation(ParticipationRole role, int units, PaymentStatus paymentStatus,
                                  ReceiptStatus receiptStatus) {
    }

    private PostResponses() {
    }
}
