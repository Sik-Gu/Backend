package Sikgu.com.example.Web.domain.review.dto.response;

import Sikgu.com.example.Web.domain.participation.enums.ParticipationRole;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.review.enums.ReviewTag;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 후기 API 응답 DTO 모음 (docs/API.md 10장).
 */
public final class ReviewResponses {

    /** GET /api/posts/{postId}/reviews/targets */
    public record ReviewTargetsResponse(
            Long postId,
            String postTitle,
            PostStatus postStatus,
            ParticipationRole myRole,
            LocalDateTime settledAt,
            LocalDateTime writableUntil,
            boolean canWrite,
            List<ReviewTarget> targets
    ) {
    }

    public record ReviewTarget(Long userId, String nickname, ParticipationRole role, int units, boolean written) {
    }

    /** POST /api/posts/{postId}/reviews */
    public record ReviewWriteResponse(List<Long> createdReviewIds) {
    }

    /** GET /api/users/{userId}/reviews 항목 */
    public record ReceivedReview(
            Long reviewId,
            Long reviewerId,
            String reviewerNickname,
            ParticipationRole reviewerRole,
            Long postId,
            String postTitle,
            int rating,
            List<ReviewTag> tags,
            String content,
            LocalDate createdAt
    ) {
    }

    private ReviewResponses() {
    }
}
