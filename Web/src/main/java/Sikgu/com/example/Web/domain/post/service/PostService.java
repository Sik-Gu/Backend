package Sikgu.com.example.Web.domain.post.service;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.participation.service.TradeStatsReader;
import Sikgu.com.example.Web.domain.post.dto.request.PostCreateRequest;
import Sikgu.com.example.Web.domain.post.dto.request.TradePlaceRequest;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.HostSummary;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.MyParticipation;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.ParticipantSummary;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCreateResponse;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostDetailResponse;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.StatusTimeline;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.Category;
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
 * 공구 등록·상세 (docs/API.md 6장)
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
        TradePlaceRequest place = request.tradePlace();
        Post post = Post.create(host, request.title(), request.content(), request.productName(),
                request.productLink(), request.category(), request.baseUnit(), request.totalUnits(),
                request.totalPrice(), request.deadlineAt(), place.name(), place.dongName(),
                place.latitude(), place.longitude());
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
                GeoUtils.distanceKm(viewer.getLatitude(), viewer.getLongitude(),
                        post.getTradeLatitude(), post.getTradeLongitude()),
                post.getTradePlaceName(), post.getDeadlineAt(), post.getViewCount() + 1, post.getCreatedAt(),
                new HostSummary(host.getId(), host.getNickname(), host.ratingValue(), host.getTradeCount(),
                        mainCategories.getOrDefault(host.getId(), List.of())),
                participations.stream().map(pa -> new ParticipantSummary(
                        pa.getUser().getId(), pa.getUser().getNickname(), pa.getRole(), pa.getUnits(),
                        pa.getUser().ratingValue(), pa.getUser().getTradeCount(),
                        mainCategories.getOrDefault(pa.getUser().getId(), List.of()))).toList(),
                mine == null ? null : new MyParticipation(mine.getRole(), mine.getUnits(),
                        mine.getPaymentStatus(), mine.getReceiptStatus()),
                post.getStatus() == PostStatus.RECRUITING ? post.remainingUnits() : 0,
                // 채팅방 ID는 호스트·참여자에게만
                mine == null ? null : chatClient.findRoomId(postId).orElse(null));
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

    static StatusTimeline timeline(Post post) {
        return new StatusTimeline(post.getRecruitedAt(), post.getOrderedAt(), post.getShippedAt(),
                post.getDistributedAt(), post.getSettledAt(), post.getCancelledAt());
    }
}
