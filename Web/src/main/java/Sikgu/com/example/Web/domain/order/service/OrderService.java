package Sikgu.com.example.Web.domain.order.service;

import Sikgu.com.example.Web.domain.order.dto.request.OrderRequests.DistributionRequest;
import Sikgu.com.example.Web.domain.order.dto.request.OrderRequests.OrderRegisterRequest;
import Sikgu.com.example.Web.domain.order.dto.request.OrderRequests.ShippingRequest;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.Adjustment;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.AdjustmentType;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.DistributionResponse;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.MyAdjustment;
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
import Sikgu.com.example.Web.integration.point.PointClient;
import Sikgu.com.example.Web.integration.point.PointClient.AdjustmentResult;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 영수증 등록(차액 정산)·주문 조회·배송 시작·나눔 일정 입력 (docs/API.md 7장)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final PostRepository postRepository;
    private final PostOrderRepository orderRepository;
    private final ParticipationRepository participationRepository;
    private final PointClient pointClient;
    private final ChatClient chatClient;
    private final Clock clock;

    /**
     * 영수증 등록 + 주문완료. 영수증 금액이 모집 금액과 다르면 차액을 단위 비율대로 정산한다 (D9).
     * <ul>
     *   <li>더 쌈: 호스트 포함 각자에게 환급</li>
     *   <li>더 비쌈: 각자에게 추가 청구 (상한 없음). 잔액이 모자라면 미수금</li>
     *   <li>나눗셈 나머지는 호스트에게 몰아서 처리</li>
     * </ul>
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

        PostOrder order = orderRepository.save(
                PostOrder.register(post, request.receiptAmount(), request.receiptImageUrls(), now));
        post.markOrdered(now);

        int difference = request.receiptAmount() - post.getTotalPrice();
        List<Adjustment> adjustments = difference == 0 ? List.of() : adjust(post, difference);
        chatClient.sendSystemMessage(postId, orderMessage(difference));

        return new OrderRegisterResponse(order.getId(), post.getStatus(), order.getReceiptAmount(),
                post.getTotalPrice(), difference == 0, difference, adjustmentType(difference), adjustments);
    }

    /** 주문 정보 조회 (호스트·참여자) */
    public OrderDetailResponse get(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
        List<Participation> participations = participationRepository.findActiveByPostId(postId);
        Participation mine = participations.stream()
                .filter(pa -> pa.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_PARTICIPANT));
        PostOrder order = orderRepository.findByPostId(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));

        int difference = order.getReceiptAmount() - post.getTotalPrice();
        long myAmount = valueOrZero(mine.getAdjustmentAmount());
        long myDebt = valueOrZero(mine.getAdjustmentDebtAmount());
        return new OrderDetailResponse(order.getId(), order.getCourier(), order.getTrackingNumber(),
                order.getReceiptAmount(), post.getTotalPrice(), difference == 0, difference,
                adjustmentType(difference),
                new MyAdjustment(myAmount, Math.max(myAmount, 0) - myDebt, myDebt),
                order.receiptImageUrls(), order.getRegisteredAt(), order.getDistributeAt(),
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
     * 나눔 일정·장소 입력 → 바로 DISTRIBUTED (D6). 호스트의 별도 '나눔완료' 버튼은 없다.
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
        order.scheduleDistribution(request.distributeAt(), request.distributePlace());

        participationRepository.findActiveByPostId(postId).stream()
                .filter(Participation::isHost)
                .forEach(host -> host.confirmReceipt(now, false));
        chatClient.sendSystemMessage(postId, "나눔 일정이 정해졌어요: " + request.distributePlace()
                + ". 물건을 받으면 '받았어요'를 눌러 주세요.");

        return new DistributionResponse(post.getStatus(), post.getDistributedAt(),
                ReceiptPolicy.autoConfirmAt(order.getDistributeAt()));
    }

    /** 차액을 단위 비율로 나눠 포인트 정산을 요청한다. 나머지는 호스트에게 */
    private List<Adjustment> adjust(Post post, int difference) {
        List<Participation> participations = participationRepository.findActiveByPostId(post.getId());
        long abs = Math.abs((long) difference);
        long sign = Long.signum(difference);

        long[] shares = new long[participations.size()];
        long distributed = 0;
        int hostIndex = 0;
        for (int i = 0; i < participations.size(); i++) {
            shares[i] = abs * participations.get(i).getUnits() / post.getTotalUnits();
            distributed += shares[i];
            if (participations.get(i).isHost()) {
                hostIndex = i;
            }
        }
        shares[hostIndex] += abs - distributed;

        List<Adjustment> adjustments = new ArrayList<>();
        for (int i = 0; i < participations.size(); i++) {
            Participation participation = participations.get(i);
            long amount = sign * shares[i];
            if (amount == 0) {
                continue;
            }
            AdjustmentResult result = pointClient.adjust(participation.getUser().getId(), post.getId(),
                    participation.getId(), amount);
            participation.applyAdjustment(amount, result.debtAmount());
            adjustments.add(new Adjustment(participation.getUser().getId(), participation.getRole(),
                    participation.getUnits(), amount, result.chargedAmount(), result.debtAmount()));
        }
        return adjustments;
    }

    /** 아직 보류 중인 포인트 (정산·취소 후 0). 포인트 기능 연동 전까지 참여 정보로 계산한다 */
    private static long heldPoint(Post post, List<Participation> participations) {
        if (post.getStatus() == PostStatus.SETTLED || post.getStatus() == PostStatus.CANCELLED) {
            return 0;
        }
        return participations.stream()
                .mapToLong(pa -> pa.paidPoint() + valueOrZero(pa.getAdjustmentAmount())
                        - valueOrZero(pa.getAdjustmentDebtAmount()))
                .sum();
    }

    private static AdjustmentType adjustmentType(int difference) {
        if (difference == 0) {
            return AdjustmentType.NONE;
        }
        return difference < 0 ? AdjustmentType.REFUND : AdjustmentType.CHARGE;
    }

    private static String orderMessage(int difference) {
        if (difference == 0) {
            return "호스트가 영수증을 등록했어요. 주문완료!";
        }
        if (difference < 0) {
            return "호스트가 영수증을 등록했어요. 예상보다 " + (-difference) + "원 싸서 단위 비율대로 환급했어요.";
        }
        return "호스트가 영수증을 등록했어요. 예상보다 " + difference
                + "원 비싸서 단위 비율대로 추가 청구했어요. 잔액이 부족하면 충전해 주세요.";
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

    private static long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
