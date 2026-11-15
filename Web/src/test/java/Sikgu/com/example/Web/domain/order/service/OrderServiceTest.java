package Sikgu.com.example.Web.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.order.dto.request.OrderRequests.DistributionRequest;
import Sikgu.com.example.Web.domain.order.dto.request.OrderRequests.OrderRegisterRequest;
import Sikgu.com.example.Web.domain.order.dto.request.OrderRequests.ShippingRequest;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.Adjustment;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.AdjustmentType;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.DistributionResponse;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.OrderDetailResponse;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.OrderRegisterResponse;
import Sikgu.com.example.Web.domain.order.entity.PostOrder;
import Sikgu.com.example.Web.domain.order.repository.PostOrderRepository;
import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.ReceiptStatus;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.integration.chat.ChatClient;
import Sikgu.com.example.Web.integration.point.PointClient;
import Sikgu.com.example.Web.integration.point.PointClient.AdjustmentResult;
import Sikgu.com.example.Web.support.Fixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 생수 6단위(단위당 4,840원, 모집 금액 29,040원): 호스트 2단위, 참여자 A 1단위, 참여자 B 3단위
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    PostRepository postRepository;
    @Mock
    PostOrderRepository orderRepository;
    @Mock
    ParticipationRepository participationRepository;
    @Mock
    PointClient pointClient;
    @Mock
    ChatClient chatClient;

    OrderService service;
    User host = Fixtures.user(10L);
    User a = Fixtures.user(20L);
    User b = Fixtures.user(30L);
    Post post;
    Participation hostPa;
    Participation aPa;
    Participation bPa;

    @BeforeEach
    void setUp() {
        service = new OrderService(postRepository, orderRepository, participationRepository, pointClient, chatClient,
                CLOCK);
        post = Fixtures.post(3L, host, NOW.plusDays(2));
        post.addParticipant(2);
        post.addParticipant(1);
        post.addParticipant(3);
        post.markRecruited(NOW);
        hostPa = withId(Participation.host(post, host, 2, NOW), 1L);
        aPa = withId(Participation.participant(post, a, 1, NOW), 2L);
        bPa = withId(Participation.participant(post, b, 3, NOW), 3L);

        given(postRepository.findByIdForUpdate(3L)).willReturn(Optional.of(post));
        given(postRepository.findById(3L)).willReturn(Optional.of(post));
        given(participationRepository.findActiveByPostId(3L)).willReturn(List.of(hostPa, aPa, bPa));
        given(orderRepository.save(any(PostOrder.class))).willAnswer(inv -> inv.getArgument(0));
        // 기본: 잔액이 충분해서 청구 금액이 모두 빠짐
        given(pointClient.adjust(any(), any(), any(), anyLong())).willAnswer(inv -> {
            long amount = inv.getArgument(3);
            return new AdjustmentResult(amount, Math.max(amount, 0), 0);
        });
    }

    @Test
    void 금액이_같으면_차액_정산_없이_주문완료() {
        OrderRegisterResponse response = service.register(10L, 3L, new OrderRegisterRequest(29040, List.of("/r.jpg")));

        assertThat(response.postStatus()).isEqualTo(PostStatus.ORDERED);
        assertThat(response.amountMatched()).isTrue();
        assertThat(response.adjustmentType()).isEqualTo(AdjustmentType.NONE);
        assertThat(post.getOrderedAt()).isEqualTo(NOW);
        verify(pointClient, never()).adjust(any(), any(), any(), anyLong());
    }

    @Test
    void 더_비싸면_단위_비율로_추가_청구하고_나머지는_호스트에게() {
        // 차액 1,000원: 호스트 2/6=333, A 1/6=166, B 3/6=500 → 합 999, 나머지 1원은 호스트 → 334
        OrderRegisterResponse response = service.register(10L, 3L, new OrderRegisterRequest(30040, List.of("/r.jpg")));

        assertThat(response.difference()).isEqualTo(1000);
        assertThat(response.adjustmentType()).isEqualTo(AdjustmentType.CHARGE);
        assertThat(response.adjustments()).extracting(Adjustment::userId, Adjustment::amount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, 334L),
                        org.assertj.core.groups.Tuple.tuple(20L, 166L),
                        org.assertj.core.groups.Tuple.tuple(30L, 500L));
        assertThat(response.adjustments().stream().mapToLong(Adjustment::amount).sum()).isEqualTo(1000);
        verify(pointClient).adjust(10L, 3L, 1L, 334L);
        assertThat(hostPa.getAdjustmentAmount()).isEqualTo(334L);
    }

    @Test
    void 더_싸면_단위_비율로_환급한다() {
        // 차액 -600원: 호스트 200, A 100, B 300 (나머지 없음)
        OrderRegisterResponse response = service.register(10L, 3L, new OrderRegisterRequest(28440, List.of("/r.jpg")));

        assertThat(response.adjustmentType()).isEqualTo(AdjustmentType.REFUND);
        assertThat(response.adjustments()).extracting(Adjustment::amount).containsExactly(-200L, -100L, -300L);
        verify(pointClient).adjust(30L, 3L, 3L, -300L);
    }

    @Test
    void 추가_청구에서_잔액이_모자라면_미수금을_기록한다() {
        given(pointClient.adjust(20L, 3L, 2L, 166L)).willReturn(new AdjustmentResult(166L, 66L, 100L));

        OrderRegisterResponse response = service.register(10L, 3L, new OrderRegisterRequest(30040, List.of("/r.jpg")));

        Adjustment adjA = response.adjustments().get(1);
        assertThat(adjA.chargedAmount()).isEqualTo(66L);
        assertThat(adjA.debtAmount()).isEqualTo(100L);
        assertThat(aPa.getAdjustmentDebtAmount()).isEqualTo(100L);
    }

    @Test
    void 호스트가_아니면_AUTH403() {
        assertThatThrownBy(() -> service.register(20L, 3L, new OrderRegisterRequest(29040, List.of("/r.jpg"))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.NOT_POST_HOST);
    }

    @Test
    void 모집완료가_아니면_POST409() {
        Post recruiting = Fixtures.post(4L, host, NOW.plusDays(2));
        given(postRepository.findByIdForUpdate(4L)).willReturn(Optional.of(recruiting));

        assertThatThrownBy(() -> service.register(10L, 4L, new OrderRegisterRequest(29040, List.of("/r.jpg"))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_POST_STATUS);
    }

    @Test
    void 이미_주문을_등록했으면_ORDER4091() {
        given(orderRepository.existsByPostId(3L)).willReturn(true);

        assertThatThrownBy(() -> service.register(10L, 3L, new OrderRegisterRequest(29040, List.of("/r.jpg"))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.ORDER_ALREADY_EXISTS);
    }

    @Test
    void 주문_조회는_내_차액과_보관중_포인트를_보여준다() {
        service.register(10L, 3L, new OrderRegisterRequest(30040, List.of("/r.jpg")));
        given(orderRepository.findByPostId(3L))
                .willReturn(Optional.of(PostOrder.register(post, 30040, List.of("/r.jpg"), NOW)));

        OrderDetailResponse detail = service.get(20L, 3L);

        assertThat(detail.myAdjustment().amount()).isEqualTo(166L);
        assertThat(detail.receiptImageUrls()).containsExactly("/r.jpg");
        assertThat(detail.heldPoint()).isEqualTo(29040L + 1000L);
        assertThat(detail.autoConfirmAt()).isNull();
    }

    @Test
    void 참여자가_아니면_주문을_볼_수_없다() {
        assertThatThrownBy(() -> service.get(99L, 3L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.NOT_PARTICIPANT);
    }

    @Test
    void 배송_시작은_주문완료에서만() {
        PostOrder order = PostOrder.register(post, 29040, List.of("/r.jpg"), NOW);
        given(orderRepository.findByPostId(3L)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> service.ship(10L, 3L, new ShippingRequest(null, null)))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_POST_STATUS);

        post.markOrdered(NOW);
        assertThat(service.ship(10L, 3L, new ShippingRequest("CJ대한통운", "6123-4567")).postStatus())
                .isEqualTo(PostStatus.SHIPPING);
        assertThat(order.getCourier()).isEqualTo("CJ대한통운");
    }

    @Test
    void 택배사와_송장은_둘_다_보내거나_둘_다_비워야_한다() {
        assertThat(new ShippingRequest(null, null).isBothOrNone()).isTrue();
        assertThat(new ShippingRequest("CJ", "123").isBothOrNone()).isTrue();
        assertThat(new ShippingRequest("CJ", " ").isBothOrNone()).isFalse();
    }

    @Test
    void 나눔_일정을_입력하면_DISTRIBUTED가_되고_호스트는_바로_받은_것으로_처리한다() {
        post.markOrdered(NOW);
        post.markShipping(NOW);
        PostOrder order = PostOrder.register(post, 29040, List.of("/r.jpg"), NOW);
        given(orderRepository.findByPostId(3L)).willReturn(Optional.of(order));
        LocalDateTime distributeAt = NOW.plusDays(1);

        DistributionResponse response = service.scheduleDistribution(10L, 3L,
                new DistributionRequest(distributeAt, "정문 앞"));

        assertThat(response.postStatus()).isEqualTo(PostStatus.DISTRIBUTED);
        assertThat(response.autoConfirmAt()).isEqualTo(distributeAt.plusDays(3));
        assertThat(order.getDistributePlace()).isEqualTo("정문 앞");
        assertThat(hostPa.getReceiptStatus()).isEqualTo(ReceiptStatus.RECEIVED);
        assertThat(aPa.getReceiptStatus()).isEqualTo(ReceiptStatus.WAITING);
    }

    private static Participation withId(Participation participation, Long id) {
        ReflectionTestUtils.setField(participation, "id", id);
        return participation;
    }
}
