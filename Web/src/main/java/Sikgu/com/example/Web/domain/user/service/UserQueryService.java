package Sikgu.com.example.Web.domain.user.service;

import Sikgu.com.example.Web.domain.block.repository.BlockRepository;
import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.PaymentStatus;
import Sikgu.com.example.Web.domain.participation.enums.ReceiptStatus;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.participation.service.TradeStatsReader;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.DistanceRange;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.service.PostQueryService;
import Sikgu.com.example.Web.domain.review.entity.Review;
import Sikgu.com.example.Web.domain.review.enums.ReviewTag;
import Sikgu.com.example.Web.domain.review.repository.ReviewRepository;
import Sikgu.com.example.Web.domain.review.service.ReviewService;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.LocationResponse;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.MeResponse;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.MyPostCard;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.PointInfo;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.PostCounts;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.ProfileResponse;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.RecentReview;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.RecentTrade;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.ReviewProgress;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.TagCount;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.enums.MyPostEnums.ActionRequired;
import Sikgu.com.example.Web.domain.user.enums.MyPostEnums.MyPostFilter;
import Sikgu.com.example.Web.domain.user.enums.MyPostEnums.MyPostTab;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.PageResponse;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.global.util.GeoUtils;
import Sikgu.com.example.Web.integration.point.PointClient;
import Sikgu.com.example.Web.integration.point.PointClient.PointSummary;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 정보·내 공구함·유저 프로필 (docs/API.md 4장)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    static final int RECENT_LIMIT = 3;

    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final ReviewRepository reviewRepository;
    private final BlockRepository blockRepository;
    private final TradeStatsReader tradeStatsReader;
    private final PointClient pointClient;
    private final Clock clock;

    /** 내 정보: 프로필 + 공구 수 + 포인트 */
    public MeResponse getMe(Long userId) {
        User user = getUser(userId);
        List<MyPostItem> items = myPostItems(user);
        PointSummary point = pointClient.getSummary(userId);

        int ongoing = (int) items.stream().filter(item -> !item.post().getStatus().isDone()).count();
        int actionRequired = (int) items.stream().filter(item -> item.action() != null).count();
        return new MeResponse(user.getId(), user.getNickname(), user.getSchoolEmail(),
                user.getUniversityName(),
                new LocationResponse(user.getDongName(), user.getLatitude(), user.getLongitude()),
                user.getCreatedAt() == null ? null : user.getCreatedAt().toLocalDate(),
                user.ratingValue(), reviewRepository.countByTargetId(userId), user.getTradeCount(),
                new PostCounts(ongoing, items.size() - ongoing, actionRequired),
                new PointInfo(point.availableBalance(), point.heldAmount(), point.unpaidDebt()),
                point.unpaidDebt() > 0);
    }

    /** 내 공구함. 최신 등록순 */
    public PageResponse<MyPostCard> getMyPosts(Long userId, MyPostTab tab, MyPostFilter filter, int page, int size) {
        User user = getUser(userId);
        List<MyPostCard> cards = myPostItems(user).stream()
                .filter(item -> (tab == MyPostTab.DONE) == item.post().getStatus().isDone())
                .filter(item -> matches(filter, item))
                .sorted(Comparator.comparing((MyPostItem item) -> item.post().getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> toCard(user, item))
                .toList();

        int from = Math.min(page * size, cards.size());
        int to = Math.min(from + size, cards.size());
        return PageResponse.of(cards.subList(from, to), page, size, cards.size());
    }

    /** 유저 프로필 모달. 좌표는 보내지 않고 동 이름만 (D14) */
    public ProfileResponse getProfile(Long viewerId, Long userId) {
        User user = getUser(userId);
        List<TagCount> tagCounts = reviewRepository.countTags(userId).stream()
                .map(row -> new TagCount((ReviewTag) row[0], ((Number) row[1]).longValue()))
                .toList();
        List<RecentReview> recentReviews = reviewRepository.findTop3ByTargetIdOrderByCreatedAtDesc(userId).stream()
                .map(UserQueryService::toRecentReview)
                .toList();
        List<RecentTrade> recentTrades = participationRepository
                .findTop3ByUserIdAndPaymentStatusOrderByJoinedAtDesc(userId, PaymentStatus.PAID).stream()
                .map(pa -> new RecentTrade(pa.getPost().getId(), pa.getRole(), pa.getPost().getTitle(),
                        pa.getPost().getStatus(), pa.getPost().getSettledAt()))
                .toList();

        return new ProfileResponse(user.getId(), user.getNickname(), user.getUniversityName(), user.getDongName(),
                user.getCreatedAt() == null ? null : user.getCreatedAt().toLocalDate(), user.ratingValue(),
                reviewRepository.countByTargetId(userId), user.getTradeCount(),
                tradeStatsReader.mainCategories(List.of(userId)).getOrDefault(userId, List.of()),
                tagCounts, recentReviews, recentTrades,
                blockRepository.existsByBlockerIdAndBlockedId(viewerId, userId));
    }

    // ---------- 내 공구함 계산 ----------

    /** 내 공구 한 건과 그에 대한 할 일·후기 진행 */
    private record MyPostItem(Participation mine, Post post, ActionRequired action, ReviewProgress reviewProgress) {
    }

    private List<MyPostItem> myPostItems(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        return participationRepository.findMyPosts(user.getId()).stream()
                .map(mine -> toItem(user, mine, now))
                .toList();
    }

    private MyPostItem toItem(User user, Participation mine, LocalDateTime now) {
        Post post = mine.getPost();
        ReviewProgress reviewProgress = null;
        boolean reviewPending = false;
        if (post.getStatus() == PostStatus.SETTLED && mine.isActive()) {
            reviewProgress = reviewProgress(user.getId(), mine, post);
            reviewPending = reviewProgress.written() < reviewProgress.total()
                    && now.isBefore(ReviewService.writableUntil(post));
        }
        return new MyPostItem(mine, post, actionRequired(user.getId(), mine, post, reviewPending), reviewProgress);
    }

    /** 내가 해야 할 일. 미수금 납부가 가장 급하다 */
    private ActionRequired actionRequired(Long userId, Participation mine, Post post, boolean reviewPending) {
        if (!mine.isActive()) {
            return null;
        }
        if (pointClient.hasUnpaidDebt(userId, post.getId())) {
            return ActionRequired.PAY_DEBT;
        }
        if (mine.isHost()) {
            switch (post.getStatus()) {
                case RECRUITED -> {
                    if (!post.hasDistributionPlace()) {
                        return ActionRequired.PROPOSE_PLACE;
                    }
                    // 아직 동의하지 않은 참여자가 있으면 기다린다
                    boolean allAgreed = participationRepository.countByPostIdAndPaymentStatusAndPlaceAgreed(
                            post.getId(), PaymentStatus.PAID, false) == 0;
                    return allAgreed ? ActionRequired.ORDER : null;
                }
                case ORDERED -> {
                    return ActionRequired.SHIP;
                }
                case SHIPPING -> {
                    return ActionRequired.DISTRIBUTE;
                }
                default -> {
                }
            }
        } else if ((post.getStatus() == PostStatus.RECRUITING || post.getStatus() == PostStatus.RECRUITED)
                && post.hasDistributionPlace() && !mine.isPlaceAgreed()) {
            return ActionRequired.AGREE_PLACE;
        } else if (post.getStatus() == PostStatus.DISTRIBUTED
                && mine.getReceiptStatus() == ReceiptStatus.WAITING) {
            return ActionRequired.CONFIRM_RECEIPT;
        }
        return reviewPending ? ActionRequired.WRITE_REVIEW : null;
    }

    /** 후기 진행: 호스트는 참여자 전원, 참여자는 호스트 1명이 대상 */
    private ReviewProgress reviewProgress(Long userId, Participation mine, Post post) {
        int total = mine.isHost() ? Math.max(post.getParticipantCount() - 1, 0) : 1;
        Set<Long> written = new HashSet<>(reviewRepository.findTargetIdsByWriterAndPost(userId, post.getId()));
        return new ReviewProgress(Math.min(written.size(), total), total);
    }

    private static boolean matches(MyPostFilter filter, MyPostItem item) {
        return switch (filter) {
            case ALL -> true;
            case HOST -> item.mine().isHost();
            case JOINED -> !item.mine().isHost();
            case ACTION_REQUIRED -> item.action() != null;
        };
    }

    private static MyPostCard toCard(User user, MyPostItem item) {
        Post post = item.post();
        DistanceRange distance = DistanceRange.of(GeoUtils.distanceMeters(user.getLatitude(), user.getLongitude(),
                post.getHostLatitude(), post.getHostLongitude()));
        return new MyPostCard(PostQueryService.toCard(post, distance), item.mine().getRole(),
                item.mine().getUnits(), item.action(), item.reviewProgress());
    }

    private static RecentReview toRecentReview(Review review) {
        return new RecentReview(review.getWriter().getNickname(), review.getPost().getTitle(), review.getRating(),
                review.getContent(), review.getCreatedAt().toLocalDate());
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }
}
