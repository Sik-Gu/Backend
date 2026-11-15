package Sikgu.com.example.Web.domain.review.service;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.ParticipationRole;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.review.dto.request.ReviewWriteRequest;
import Sikgu.com.example.Web.domain.review.dto.request.ReviewWriteRequest.ReviewItem;
import Sikgu.com.example.Web.domain.review.dto.response.ReviewResponses.ReceivedReview;
import Sikgu.com.example.Web.domain.review.dto.response.ReviewResponses.ReviewTarget;
import Sikgu.com.example.Web.domain.review.dto.response.ReviewResponses.ReviewTargetsResponse;
import Sikgu.com.example.Web.domain.review.dto.response.ReviewResponses.ReviewWriteResponse;
import Sikgu.com.example.Web.domain.review.entity.Review;
import Sikgu.com.example.Web.domain.review.enums.ReviewTag;
import Sikgu.com.example.Web.domain.review.repository.ReviewRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.PageResponse;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 후기 (docs/API.md 10장). 참여자 → 호스트, 호스트 → 참여자 양방향.
 * 정산완료 후 3일 안에만 쓰고, 수정할 수 없다 (D12).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    /** 정산 후 후기를 쓸 수 있는 기간 (D12) */
    public static final Duration WRITABLE_FOR = Duration.ofDays(3);

    private final PostRepository postRepository;
    private final ParticipationRepository participationRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /** 평가 대상과 작성 여부 */
    public ReviewTargetsResponse getTargets(Long userId, Long postId) {
        Post post = getPost(postId);
        List<Participation> participations = participationRepository.findActiveByPostId(postId);
        Participation mine = findMine(participations, userId);

        Set<Long> written = new HashSet<>(reviewRepository.findTargetIdsByWriterAndPost(userId, postId));
        List<ReviewTarget> targets = targetsOf(mine, participations).stream()
                .map(pa -> new ReviewTarget(pa.getUser().getId(), pa.getUser().getNickname(), pa.getRole(),
                        pa.getUnits(), written.contains(pa.getUser().getId())))
                .toList();

        LocalDateTime writableUntil = writableUntil(post);
        boolean canWrite = isWritable(post) && targets.stream().anyMatch(target -> !target.written());
        return new ReviewTargetsResponse(post.getId(), post.getTitle(), post.getStatus(), mine.getRole(),
                post.getSettledAt(), writableUntil, canWrite, targets);
    }

    /** 후기 작성 (여러 명에게 한 번에). 하나라도 잘못되면 전부 저장하지 않는다 */
    @Transactional
    public ReviewWriteResponse write(Long userId, Long postId, ReviewWriteRequest request) {
        Post post = getPost(postId);
        List<Participation> participations = participationRepository.findActiveByPostId(postId);
        Participation mine = findMine(participations, userId);
        if (!isWritable(post)) {
            throw new GeneralException(ErrorStatus.REVIEW_PERIOD_CLOSED);
        }

        Map<Long, User> allowedTargets = targetsOf(mine, participations).stream()
                .collect(Collectors.toMap(pa -> pa.getUser().getId(), Participation::getUser));
        Set<Long> written = new HashSet<>(reviewRepository.findTargetIdsByWriterAndPost(userId, postId));
        Set<Long> inRequest = new HashSet<>();

        List<Review> reviews = request.reviews().stream().map(item -> {
            User target = allowedTargets.get(item.targetUserId());
            if (target == null) {
                throw new GeneralException(ErrorStatus.REVIEW_TARGET_INVALID); // 본인, 평가 방향이 아닌 사람
            }
            if (!inRequest.add(item.targetUserId())) {
                throw new GeneralException(ErrorStatus.BAD_REQUEST); // 한 요청에 같은 대상이 두 번
            }
            if (written.contains(item.targetUserId())) {
                throw new GeneralException(ErrorStatus.REVIEW_DUPLICATED);
            }
            return Review.write(mine.getUser(), post, target, item.rating(), toTagSet(item), item.content());
        }).toList();

        try {
            reviewRepository.saveAllAndFlush(reviews); // 동시에 같은 후기를 쓰면 UNIQUE 제약으로 막는다
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(ErrorStatus.REVIEW_DUPLICATED);
        }
        reviews.forEach(review -> refreshRating(review.getTarget()));

        return new ReviewWriteResponse(reviews.stream().map(Review::getId).toList());
    }

    /** 받은 후기 목록 (최신순) */
    public PageResponse<ReceivedReview> getReceived(Long targetId, int page, int size) {
        if (!userRepository.existsById(targetId)) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }
        return PageResponse.of(
                reviewRepository.findByTargetIdOrderByCreatedAtDesc(targetId, PageRequest.of(page, size)),
                ReviewService::toReceived);
    }

    /** 후기 작성 기한 = 정산 시각 + 3일. 정산 전이면 null */
    public static LocalDateTime writableUntil(Post post) {
        return post.getSettledAt() == null ? null : post.getSettledAt().plus(WRITABLE_FOR);
    }

    private boolean isWritable(Post post) {
        return post.getStatus() == PostStatus.SETTLED
                && LocalDateTime.now(clock).isBefore(writableUntil(post));
    }

    /** 참여자의 대상은 호스트 1명, 호스트의 대상은 참여자 전원 (참여자끼리는 평가하지 않음) */
    static List<Participation> targetsOf(Participation mine, List<Participation> participations) {
        ParticipationRole targetRole = mine.isHost() ? ParticipationRole.PARTICIPANT : ParticipationRole.HOST;
        return participations.stream().filter(pa -> pa.getRole() == targetRole).toList();
    }

    private void refreshRating(User target) {
        Double average = reviewRepository.averageRating(target.getId());
        target.updateRating(average == null ? null
                : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP));
    }

    private static ReceivedReview toReceived(Review review) {
        ParticipationRole reviewerRole = review.getPost().getHost().getId().equals(review.getWriter().getId())
                ? ParticipationRole.HOST : ParticipationRole.PARTICIPANT;
        return new ReceivedReview(review.getId(), review.getWriter().getId(), review.getWriter().getNickname(),
                reviewerRole, review.getPost().getId(), review.getPost().getTitle(), review.getRating(),
                List.copyOf(review.getTags()), review.getContent(), review.getCreatedAt().toLocalDate());
    }

    private static Set<ReviewTag> toTagSet(ReviewItem item) {
        if (item.tags() == null || item.tags().isEmpty()) {
            return EnumSet.noneOf(ReviewTag.class);
        }
        if (new HashSet<>(item.tags()).size() != item.tags().size()) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST); // 같은 태그 중복
        }
        return EnumSet.copyOf(item.tags());
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
    }

    private static Participation findMine(List<Participation> participations, Long userId) {
        return participations.stream()
                .filter(pa -> pa.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_PARTICIPANT));
    }
}
