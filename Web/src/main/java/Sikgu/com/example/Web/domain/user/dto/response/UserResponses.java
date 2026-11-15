package Sikgu.com.example.Web.domain.user.dto.response;

import Sikgu.com.example.Web.domain.participation.enums.ParticipationRole;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCard;
import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.review.enums.ReviewTag;
import Sikgu.com.example.Web.domain.user.enums.MyPostEnums.ActionRequired;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원 API 응답 DTO 모음.
 */
public final class UserResponses {

    /** PATCH /api/users/me/location */
    public record LocationResponse(String dongName, Double latitude, Double longitude) {
    }

    /** GET /api/users/me */
    public record MeResponse(
            Long userId,
            String loginId,
            String nickname,
            String email,
            String universityName,
            LocationResponse location,
            LocalDate joinedAt,
            Double rating,
            long reviewCount,
            int tradeCount,
            PostCounts postCounts,
            PointInfo point,
            boolean restricted
    ) {
    }

    public record PostCounts(int ongoing, int done, int actionRequired) {
    }

    /** 포인트 기능에서 가져온 값 (D9: unpaidDebt > 0이면 공구 참여·등록 제한) */
    public record PointInfo(long availableBalance, long heldAmount, long unpaidDebt) {
    }

    /** GET /api/users/me/posts 항목 = 목록 카드 + 내 정보 */
    public record MyPostCard(
            PostCard post,
            ParticipationRole myRole,
            int myUnits,
            ActionRequired actionRequired,
            ReviewProgress reviewProgress
    ) {
    }

    /** 완료 탭의 정산완료 공구에서만 값이 있음 */
    public record ReviewProgress(int written, int total) {
    }

    /** GET /api/users/{userId} */
    public record ProfileResponse(
            Long userId,
            String nickname,
            String universityName,
            String dongName,
            LocalDate joinedAt,
            Double rating,
            long reviewCount,
            int tradeCount,
            List<Category> mainCategories,
            List<TagCount> tagCounts,
            List<RecentReview> recentReviews,
            List<RecentTrade> recentTrades,
            boolean isBlocked
    ) {
    }

    public record TagCount(ReviewTag tag, long count) {
    }

    public record RecentReview(String reviewerNickname, String postTitle, int rating, String content,
                               LocalDate createdAt) {
    }

    public record RecentTrade(Long postId, ParticipationRole role, String title, PostStatus status,
                              LocalDateTime settledAt) {
    }

    private UserResponses() {
    }
}
