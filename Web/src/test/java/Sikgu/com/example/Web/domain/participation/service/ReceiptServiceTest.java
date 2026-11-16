package Sikgu.com.example.Web.domain.participation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.ReceiptStatus;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.participation.service.ReceiptService.ReceiptResult;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.integration.chat.ChatClient;
import Sikgu.com.example.Web.integration.point.PointClient;
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

/** 받았어요 → 전원 수령 시 정산, 자동 수령 처리 (D10). SettlementService는 실제 객체로 함께 확인 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReceiptServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    PostRepository postRepository;
    @Mock
    ParticipationRepository participationRepository;
    @Mock
    PointClient pointClient;
    @Mock
    ChatClient chatClient;

    ReceiptService service;
    User host = Fixtures.user(10L);
    User a = Fixtures.user(20L);
    User b = Fixtures.user(30L);
    Post post;
    Participation hostPa;
    Participation aPa;
    Participation bPa;

    @BeforeEach
    void setUp() {
        SettlementService settlement = new SettlementService(participationRepository, pointClient, chatClient);
        service = new ReceiptService(postRepository, participationRepository, settlement, CLOCK);

        post = Fixtures.post(3L, host, NOW.plusDays(2));
        post.addParticipant(2);
        post.addParticipant(1);
        post.addParticipant(3);
        post.markRecruited(NOW);
        post.markOrdered(NOW);
        post.markShipping(NOW);
        post.markDistributed(NOW);
        hostPa = Participation.host(post, host, 2, NOW);
        hostPa.confirmReceipt(NOW, false); // 나눔 일정 입력 때 호스트는 받은 것으로 처리
        aPa = Participation.participant(post, a, 1, NOW);
        bPa = Participation.participant(post, b, 3, NOW);

        given(postRepository.findByIdForUpdate(3L)).willReturn(Optional.of(post));
        given(participationRepository.findActiveByPostId(3L)).willReturn(List.of(hostPa, aPa, bPa));
        given(participationRepository.findByPostIdAndUserId(3L, 20L)).willReturn(Optional.of(aPa));
        given(participationRepository.findByPostIdAndUserId(3L, 30L)).willReturn(Optional.of(bPa));
    }

    @Test
    void 아직_안_받은_사람이_있으면_정산하지_않는다() {
        ReceiptResult result = service.confirm(20L, 3L);

        assertThat(result).isEqualTo(new ReceiptResult(ReceiptStatus.RECEIVED, PostStatus.DISTRIBUTED));
        assertThat(aPa.getReceivedAt()).isEqualTo(NOW);
        assertThat(aPa.isAutoConfirmed()).isFalse();
        verify(pointClient, never()).settle(any(), any());
    }

    @Test
    void 마지막_사람이_받으면_정산하고_거래_횟수를_올리고_채팅방을_닫는다() {
        aPa.confirmReceipt(NOW, false);

        ReceiptResult result = service.confirm(30L, 3L);

        assertThat(result.postStatus()).isEqualTo(PostStatus.SETTLED);
        assertThat(post.getSettledAt()).isEqualTo(NOW);
        verify(pointClient).settle(3L, 10L);
        verify(chatClient).close(3L);
        assertThat(host.getTradeCount()).isEqualTo(1);
        assertThat(a.getTradeCount()).isEqualTo(1);
        assertThat(b.getTradeCount()).isEqualTo(1);
    }

    @Test
    void 문의중인_사람이_있으면_정산하지_않는다() {
        aPa.startInquiry();

        service.confirm(30L, 3L);

        assertThat(post.getStatus()).isEqualTo(PostStatus.DISTRIBUTED);
        verify(pointClient, never()).settle(any(), any());
    }

    @Test
    void 이미_받았으면_PART4093() {
        aPa.confirmReceipt(NOW, false);

        assertThatThrownBy(() -> service.confirm(20L, 3L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.ALREADY_RECEIVED);
    }

    @Test
    void 문의중이면_스스로_받았어요를_누를_수_없다() {
        aPa.startInquiry();

        assertThatThrownBy(() -> service.confirm(20L, 3L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INQUIRY_IN_PROGRESS);
    }

    @Test
    void 나눔_진행_중이_아니면_POST409() {
        Post shipping = Fixtures.post(4L, host, NOW.plusDays(2));
        given(postRepository.findByIdForUpdate(4L)).willReturn(Optional.of(shipping));
        given(participationRepository.findByPostIdAndUserId(4L, 20L))
                .willReturn(Optional.of(Participation.participant(shipping, a, 1, NOW)));

        assertThatThrownBy(() -> service.confirm(20L, 4L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_POST_STATUS);
    }

    @Test
    void 참여자가_아니면_AUTH403() {
        assertThatThrownBy(() -> service.confirm(99L, 3L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.NOT_PARTICIPANT);
    }

    @Test
    void 자동_수령은_대기중인_사람만_처리하고_전원_받았으면_정산한다() {
        int count = service.autoConfirm(3L);

        assertThat(count).isEqualTo(2);
        assertThat(aPa.isAutoConfirmed()).isTrue();
        assertThat(bPa.getReceiptStatus()).isEqualTo(ReceiptStatus.RECEIVED);
        assertThat(hostPa.isAutoConfirmed()).isFalse(); // 이미 받은 호스트는 그대로
        assertThat(post.getStatus()).isEqualTo(PostStatus.SETTLED);
    }

    @Test
    void 자동_수령은_문의중인_사람을_건드리지_않아_정산되지_않는다() {
        aPa.startInquiry();

        service.autoConfirm(3L);

        assertThat(aPa.getReceiptStatus()).isEqualTo(ReceiptStatus.INQUIRING);
        assertThat(bPa.getReceiptStatus()).isEqualTo(ReceiptStatus.RECEIVED);
        assertThat(post.getStatus()).isEqualTo(PostStatus.DISTRIBUTED);
    }

    @Test
    void 이미_정산된_공구는_자동_수령하지_않는다() {
        Post settled = Fixtures.post(5L, host, NOW.plusDays(2));
        given(postRepository.findByIdForUpdate(5L)).willReturn(Optional.of(settled));

        assertThat(service.autoConfirm(5L)).isZero();
    }
}
