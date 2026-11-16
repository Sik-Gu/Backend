package Sikgu.com.example.Web.domain.order.service;

import Sikgu.com.example.Web.domain.order.dto.request.OrderRequests.DistributionRequest;
import Sikgu.com.example.Web.domain.order.dto.request.OrderRequests.OrderRegisterRequest;
import Sikgu.com.example.Web.domain.order.dto.request.OrderRequests.ShippingRequest;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.DistributionResponse;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.OrderDetailResponse;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.OrderRegisterResponse;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.ShippingResponse;
import Sikgu.com.example.Web.domain.order.entity.PostOrder;
import Sikgu.com.example.Web.domain.order.repository.PostOrderRepository;
import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.participation.service.ReceiptPolicy;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.integration.chat.ChatClient;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 영수증 등록·주문 조회·배송 시작·나눔 일시 입력. 차액 정산은 하지 않는다 (docs/API.md 7장)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final PostRepository postRepository;
    private final PostOrderRepository orderRepository;
    private final ParticipationRepository participationRepository;
    private final ChatClient chatClient;
    private final Clock clock;

    /**
     * 영수증 사진 등록 + 주문완료.
     * 주문 전 조건: 모집완료 + 나눔 장소 제안됨 + 참여자 전원(호스트 포함) 장소 동의.
     * 할인·배달비로 실제 결제 금액이 달라도 차액 정산은 하지 않는다 (게시글 가격 기준).
     */
    @Transactional
    public OrderRegisterResponse register(Long userId, Long postId, OrderRegisterRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        Post post = getPostForUpdate(postId);
        validateHost(post, userId);
        post.requireStatus(PostStatus.RECRUITED);
        if (orderRepository.existsByPostId(postId)) {
            throw new GeneralException(ErrorStatus.ORDER_ALREADY_EXISTS);
        }
        if (!post.hasDistributionPlace()) {
            throw new GeneralException(ErrorStatus.DISTRIBUTION_PLACE_NOT_PROPOSED);
        }
        boolean allAgreed = participationRepository.findActiveByPostId(postId).stream()
                .allMatch(Participation::isPlaceAgreed);
        if (!allAgreed) {
            throw new GeneralException(ErrorStatus.DISTRIBUTION_PLACE_NOT_AGREED);
        }

        PostOrder order = orderRepository.save(PostOrder.register(post, request.receiptImageUrls(), now));
        post.markOrdered(now);
        chatClient.sendSystemMessage(postId, "호스트가 영수증을 등록했어요. 주문완료! 나눔 장소: "
                + post.getDistributionPlaceName());

        return new OrderRegisterResponse(order.getId(), post.getStatus(), post.getTotalPrice(),
                post.getDistributionPlaceName());
    }

    /** 주문 정보 조회 (호스트·참여자) */
    public OrderDetailResponse get(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
        List<Participation> participations = participationRepository.findActiveByPostId(postId);
        boolean member = participations.stream().anyMatch(pa -> pa.getUser().getId().equals(userId));
        if (!member) {
            throw new GeneralException(ErrorStatus.NOT_PARTICIPANT);
        }
        PostOrder order = orderRepository.findByPostId(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));

        return new OrderDetailResponse(order.getId(), order.getCourier(), order.getTrackingNumber(),
                post.getTotalPrice(), order.receiptImageUrls(), order.getRegisteredAt(), order.getDistributeAt(),
                order.getDistributePlace(), ReceiptPolicy.autoConfirmAt(order.getDistributeAt()),
                heldPoint(post, participations));
    }

    /** 배송 시작. 오프라인 구매면 택배사·송장 없이 */
    @Transactional
    public ShippingResponse ship(Long userId, Long postId, ShippingRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        Post post = getPostForUpdate(postId);
        validateHost(post, userId);
        post.markShipping(now); // ORDERED가 아니면 POST409
        PostOrder order = orderRepository.findByPostId(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
        order.ship(blankToNull(request.courier()), blankToNull(request.trackingNumber()));
        chatClient.sendSystemMessage(postId, "호스트가 배송을 시작했어요.");
        return new ShippingResponse(post.getStatus(), post.getShippedAt());
    }

    /**
     * 나눔 일시 입력 → 바로 DISTRIBUTED (D6). 장소는 주문 전에 참여자 전원이 합의한 곳을 그대로 쓴다. 호스트의 별도 '나눔완료' 버튼은 없다.
     * 호스트 본인 몫은 바로 받은 것으로 처리하고, 참여자는 각자 '받았어요'를 누른다.
     */
    @Transactional
    public DistributionResponse scheduleDistribution(Long userId, Long postId, DistributionRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        Post post = getPostForUpdate(postId);
        validateHost(post, userId);
        post.markDistributed(now); // SHIPPING이 아니면 POST409
        PostOrder order = orderRepository.findByPostId(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
        order.scheduleDistribution(request.distributeAt(), post.getDistributionPlaceName());

        participationRepository.findActiveByPostId(postId).stream()
                .filter(Participation::isHost)
                .forEach(host -> host.confirmReceipt(now, false));
        chatClient.sendSystemMessage(postId, "나눔 일정이 정해졌어요: " + order.getDistributePlace()
                + ". 물건을 받으면 '받았어요'를 눌러 주세요.");

        return new DistributionResponse(post.getStatus(), post.getDistributedAt(), order.getDistributePlace(),
                ReceiptPolicy.autoConfirmAt(order.getDistributeAt()));
    }

    /** 아직 보류 중인 포인트 (정산·취소 후 0). 포인트 기능 연동 전까지 참여 정보로 계산한다 */
    private static long heldPoint(Post post, List<Participation> participations) {
        if (post.getStatus() == PostStatus.SETTLED || post.getStatus() == PostStatus.CANCELLED) {
            return 0;
        }
        return participations.stream().mapToLong(Participation::paidPoint).sum();
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
