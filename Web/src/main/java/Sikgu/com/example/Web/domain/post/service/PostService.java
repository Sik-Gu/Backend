package Sikgu.com.example.Web.domain.post.service;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.participation.service.TradeStatsReader;
import Sikgu.com.example.Web.domain.post.dto.request.PostCreateRequest;
import Sikgu.com.example.Web.domain.post.dto.request.PostUpdateRequest;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.DistributionPlaceInfo;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.HostSummary;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.MyParticipation;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.ParticipantSummary;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCancelResponse;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCreateResponse;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostDetailResponse;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostIdResponse;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.StatusTimeline;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.post.enums.DistanceRange;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.global.util.GeoUtils;
import Sikgu.com.example.Web.integration.chat.ChatClient;
import Sikgu.com.example.Web.integration.point.PointClient;
import Sikgu.com.example.Web.integration.point.PointClient.PaymentResult;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공구 등록·상세·수정·취소 (docs/API.md 6장)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final TradeStatsReader tradeStatsReader;
    private final PointClient pointClient;
    private final ChatClient chatClient;
    private final Clock clock;

    /**
     * 공구 등록 (한 트랜잭션).
     * 게시글 생성 → 호스트 참여 생성 → 호스트 몫 결제·보류(D8) → 채팅방 생성.
     */
    @Transactional
    public PostCreateResponse create(Long hostId, PostCreateRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        validateCreate(request, now);
        pointClient.validateNoUnpaidDebt(hostId); // D9: 미수금이 있으면 등록 불가

        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        if (!host.hasLocation()) {
            // 글에 호스트 집 위치를 저장해야 거리 필터가 동작한다
            throw new GeneralException(ErrorStatus.LOCATION_REQUIRED);
        }
        Post post = Post.create(host, request.title(), request.content(), request.productName(),
                request.productLink(), request.category(), request.baseUnit(), request.totalUnits(),
                request.totalPrice(), request.deadlineAt());
        post.replaceImages(request.imageUrls());
        postRepository.save(post);

        // 호스트도 참여 한 건 (filledUnits = hostUnits, participantCount = 1)
        post.addParticipant(request.hostUnits());
        Participation hostParticipation = participationRepository.save(
                Participation.host(post, host, request.hostUnits(), now));

        PaymentResult payment = pointClient.pay(hostId, post.getId(), hostParticipation.getId(),
                hostParticipation.paidPoint());
        Long chatRoomId = chatClient.createRoom(post.getId(), hostId, post.getTitle());

        return new PostCreateResponse(post.getId(), chatRoomId, post.getUnitPrice(),
                payment.paidPoint(), payment.balanceAfter());
    }

    /** 공구 상세. 호출할 때마다 조회수 +1 */
    @Transactional
    public PostDetailResponse getDetail(Long viewerId, Long postId) {
        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
        postRepository.increaseViewCount(postId);

        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        List<Participation> participations = participationRepository.findActiveByPostId(postId);
        Map<Long, List<Category>> mainCategories = tradeStatsReader.mainCategories(
                participations.stream().map(pa -> pa.getUser().getId()).toList());

        User host = post.getHost();
        Participation mine = participations.stream()
                .filter(pa -> pa.getUser().getId().equals(viewerId))
                .findFirst().orElse(null);

        return new PostDetailResponse(
                post.getId(), post.getTitle(), post.getProductName(), post.getProductLink(), post.getCategory(),
                post.getContent(), post.imageUrls(), post.getBaseUnit(), post.getTotalUnits(),
                post.getFilledUnits(), post.getParticipantCount(), post.getUnitPrice(), post.getTotalPrice(),
                post.getStatus(), timeline(post), post.getDongName(),
                // 좌표·정확한 거리는 내려주지 않고 구간만
                DistanceRange.of(GeoUtils.distanceMeters(viewer.getLatitude(), viewer.getLongitude(),
                        post.getHostLatitude(), post.getHostLongitude())),
                distributionPlace(post, participations, mine), post.getDeadlineAt(), post.getViewCount() + 1,
                post.getCreatedAt(),
                new HostSummary(host.getId(), host.getNickname(), host.ratingValue(), host.getTradeCount(),
                        mainCategories.getOrDefault(host.getId(), List.of())),
                participations.stream().map(pa -> new ParticipantSummary(
                        pa.getUser().getId(), pa.getUser().getNickname(), pa.getRole(), pa.getUnits(),
                        pa.getUser().ratingValue(), pa.getUser().getTradeCount(),
                        mainCategories.getOrDefault(pa.getUser().getId(), List.of()), pa.isPlaceAgreed())).toList(),
                mine == null ? null : new MyParticipation(mine.getRole(), mine.getUnits(),
                        mine.getPaymentStatus(), mine.getReceiptStatus()),
                post.getStatus() == PostStatus.RECRUITING ? post.remainingUnits() : 0,
                // 채팅방 ID는 호스트·참여자에게만
                mine == null ? null : chatClient.findRoomId(postId).orElse(null));
    }

    /**
     * 공구 수정. 모집중이고 호스트 외 참여자가 없을 때만 (참여와 동시에 일어나지 않게 게시글 락).
     * 가격·단위 관련 값은 호스트 보류 금액이 바뀌므로 수정하지 않는다 (D8).
     */
    @Transactional
    public PostIdResponse update(Long userId, Long postId, PostUpdateRequest request) {
        Post post = getPostForUpdate(postId);
        validateHost(post, userId);
        if (post.getStatus() != PostStatus.RECRUITING || post.getParticipantCount() > 1) {
            throw new GeneralException(ErrorStatus.POST_NOT_EDITABLE);
        }
        if (request.deadlineAt() != null && !request.deadlineAt().isAfter(LocalDateTime.now(clock))) {
            throw new GeneralException(ErrorStatus.DEADLINE_PASSED);
        }

        post.update(request.title(), request.productName(), request.productLink(), request.category(),
                request.content(), request.deadlineAt());
        if (request.imageUrls() != null) {
            post.replaceImages(request.imageUrls());
        }
        return new PostIdResponse(post.getId());
    }

    /**
     * 공구 취소 (주문완료 전만). 호스트 포함 참여자 전원 환급 → 채팅방 종료.
     * 락 순서: 게시글 → 지갑 (D4)
     */
    @Transactional
    public PostCancelResponse cancel(Long userId, Long postId) {
        Post post = getPostForUpdate(postId);
        validateHost(post, userId);
        post.cancel(LocalDateTime.now(clock));

        List<Participation> participations = participationRepository.findActiveByPostId(postId);
        long refundedPoint = 0;
        for (Participation participation : participations) {
            pointClient.refund(participation.getUser().getId(), postId, participation.getId());
            participation.markRefunded();
            refundedPoint += participation.paidPoint();
        }
        chatClient.close(postId);

        return new PostCancelResponse(postId, post.getStatus(), participations.size(), refundedPoint);
    }

    private Post getPostForUpdate(Long postId) {
        return postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
    }

    private static void validateHost(Post post, Long userId) {
        if (!post.isHost(userId)) {
            throw new GeneralException(ErrorStatus.NOT_POST_HOST);
        }
    }

    private void validateCreate(PostCreateRequest request, LocalDateTime now) {
        if (request.hostUnits() >= request.totalUnits()) {
            throw new GeneralException(ErrorStatus.INVALID_HOST_UNITS);
        }
        if (!request.deadlineAt().isAfter(now)) {
            throw new GeneralException(ErrorStatus.DEADLINE_PASSED);
        }
        if (request.totalPrice() % request.totalUnits() != 0) {
            // 나머지 처리 방식이 정해지지 않아서 일단 거부 (가정)
            throw new GeneralException(ErrorStatus.INDIVISIBLE_PRICE);
        }
    }

    /** 나눔 장소 합의 현황. 제안 전이면 null. participations는 참여 중인 사람 전원 (호스트 포함) */
    public static DistributionPlaceInfo distributionPlace(Post post, List<Participation> participations,
                                                   Participation mine) {
        if (!post.hasDistributionPlace()) {
            return null;
        }
        int agreed = (int) participations.stream().filter(Participation::isPlaceAgreed).count();
        return new DistributionPlaceInfo(post.getDistributionPlaceName(), post.getDistributionPlaceLatitude(),
                post.getDistributionPlaceLongitude(), post.getDistributionPlaceProposedAt(), agreed,
                participations.size(), agreed == participations.size(),
                mine == null ? null : mine.isPlaceAgreed());
    }

    static StatusTimeline timeline(Post post) {
        return new StatusTimeline(post.getRecruitedAt(), post.getOrderedAt(), post.getShippedAt(),
                post.getDistributedAt(), post.getSettledAt(), post.getCancelledAt());
    }
}
