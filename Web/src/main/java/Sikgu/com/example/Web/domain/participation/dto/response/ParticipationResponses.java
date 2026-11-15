package Sikgu.com.example.Web.domain.participation.dto.response;

import Sikgu.com.example.Web.domain.participation.enums.ParticipationRole;
import Sikgu.com.example.Web.domain.participation.enums.PaymentStatus;
import Sikgu.com.example.Web.domain.participation.enums.ReceiptStatus;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import java.util.List;

/**
 * 공구 참여 API 응답 DTO 모음 (docs/API.md 7장).
 */
public final class ParticipationResponses {

    /** POST /api/posts/{postId}/participations */
    public record JoinResponse(Long participationId, long paidPoint, long balanceAfter, PostStatus postStatus,
                               Long chatRoomId) {
    }

    /** DELETE /api/posts/{postId}/participations/me */
    public record LeaveResponse(long refundedPoint, long balanceAfter, PostStatus postStatus) {
    }

    /** GET /api/posts/{postId}/participations */
    public record ParticipationStatusResponse(int paidCount, int receivedCount, int totalCount,
                                              List<ParticipantStatus> participants) {
    }

    public record ParticipantStatus(Long userId, String nickname, ParticipationRole role, int units, long paidPoint,
                                    PaymentStatus paymentStatus, ReceiptStatus receiptStatus, boolean autoConfirmed,
                                    boolean isMe) {
    }

    private ParticipationResponses() {
    }
}
