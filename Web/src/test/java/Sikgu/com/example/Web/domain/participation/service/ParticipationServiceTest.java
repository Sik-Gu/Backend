package Sikgu.com.example.Web.domain.participation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.block.service.BlockService;
import Sikgu.com.example.Web.domain.participation.dto.response.ParticipationResponses.JoinResponse;
import Sikgu.com.example.Web.domain.participation.dto.response.ParticipationResponses.LeaveResponse;
import Sikgu.com.example.Web.domain.participation.dto.response.ParticipationResponses.ParticipationStatusResponse;
import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.LeaveReason;
import Sikgu.com.example.Web.domain.participation.enums.PaymentStatus;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.integration.chat.ChatClient;
import Sikgu.com.example.Web.integration.point.PointClient;
import Sikgu.com.example.Web.integration.point.PointClient.PaymentResult;
import Sikgu.com.example.Web.integration.point.PointClient.RefundResult;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParticipationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    PostRepository postRepository;
    @Mock
    ParticipationRepository participationRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    BlockService blockService;
    @Mock
    PointClient pointClient;
    @Mock
    ChatClient chatClient;

    ParticipationService service;
    User host = Fixtures.user(10L);
    User member = Fixtures.user(20L);
    Post post;

    @BeforeEach
    void setUp() {
        service = new ParticipationService(postRepository, participationRepository, userRepository, blockService,
                pointClient, chatClient, CLOCK);
        post = Fixtures.post(3L, host, NOW.plusDays(2));
        post.addParticipant(2); // 호스트 2단위, 남은 4단위
        given(postRepository.findByIdForUpdate(3L)).willReturn(Optional.of(post));
        given(userRepository.findById(20L)).willReturn(Optional.of(member));
        given(participationRepository.findByPostIdAndUserId(3L, 20L)).willReturn(Optional.empty());
        given(participationRepository.save(any(Participation.class))).willAnswer(inv -> inv.getArgument(0));
        given(pointClient.pay(any(), any(), any(), anyLong()))
                .willAnswer(inv -> new PaymentResult(inv.getArgument(3), 1000L));
        given(chatClient.findRoomId(3L)).willReturn(Optional.of(7L));
    }

    @Test
    void 참여하면_단위만큼_결제하고_채팅방에_입장한다() {
        JoinResponse response = service.join(20L, 3L, 2);

        assertThat(response.paidPoint()).isEqualTo(2 * 4840L);
        assertThat(response.postStatus()).isEqualTo(PostStatus.RECRUITING);
        assertThat(response.chatRoomId()).isEqualTo(7L);
        assertThat(post.getFilledUnits()).isEqualTo(4);
        assertThat(post.getParticipantCount()).isEqualTo(2);
        verify(pointClient).validateNoUnpaidDebt(20L);
        verify(chatClient).join(3L, 20L);
    }

    @Test
    void 남은_단위를_다_채우면_모집완료() {
        JoinResponse response = service.join(20L, 3L, 4);

        assertThat(response.postStatus()).isEqualTo(PostStatus.RECRUITED);
        assertThat(post.getRecruitedAt()).isEqualTo(NOW);
        verify(chatClient).sendSystemMessage(any(), anyString());
    }

    @Test
    void 남은_단위보다_많으면_PART4001이고_결제하지_않는다() {
        assertThatThrownBy(() -> service.join(20L, 3L, 5))
                .extracting("errorStatus").isEqualTo(ErrorStatus.EXCEED_REMAINING_UNITS);
        verify(pointClient, never()).pay(any(), any(), any(), anyLong());
    }

    @Test
    void 호스트_본인은_참여할_수_없다() {
        assertThatThrownBy(() -> service.join(10L, 3L, 1))
                .extracting("errorStatus").isEqualTo(ErrorStatus.ALREADY_PARTICIPATED);
    }

    @Test
    void 이미_참여_중이면_PART4091() {
        given(participationRepository.findByPostIdAndUserId(3L, 20L))
                .willReturn(Optional.of(Participation.participant(post, member, 1, NOW)));

        assertThatThrownBy(() -> service.join(20L, 3L, 1))
                .extracting("errorStatus").isEqualTo(ErrorStatus.ALREADY_PARTICIPATED);
    }

    @Test
    void 취소했던_공구에_다시_참여하면_같은_참여를_다시_쓴다() {
        Participation old = Participation.participant(post, member, 1, NOW.minusDays(1));
        old.markRefunded();
        given(participationRepository.findByPostIdAndUserId(3L, 20L)).willReturn(Optional.of(old));

        service.join(20L, 3L, 3);

        assertThat(old.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(old.getUnits()).isEqualTo(3);
        verify(participationRepository, never()).save(any());
    }

    @Test
    void 마감이_지났거나_모집중이_아니면_POST409() {
        Post expired = Fixtures.post(4L, host, NOW.minusMinutes(1));
        given(postRepository.findByIdForUpdate(4L)).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.join(20L, 4L, 1))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_POST_STATUS);
    }

    @Test
    void 호스트와_차단_관계면_PART4031() {
        given(blockService.isBlockedBetween(20L, 10L)).willReturn(true);

        assertThatThrownBy(() -> service.join(20L, 3L, 1))
                .extracting("errorStatus").isEqualTo(ErrorStatus.BLOCKED_HOST);
    }

    @Test
    void 미수금이_있으면_참여할_수_없다() {
        willThrow(new GeneralException(ErrorStatus.UNPAID_DEBT)).given(pointClient).validateNoUnpaidDebt(20L);

        assertThatThrownBy(() -> service.join(20L, 3L, 1))
                .extracting("errorStatus").isEqualTo(ErrorStatus.UNPAID_DEBT);
        verify(postRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void 참여_취소하면_환급하고_모집완료였으면_모집중으로_돌아간다() {
        post.addParticipant(4);
        post.markRecruited(NOW);
        Participation mine = Participation.participant(post, member, 4, NOW);
        given(participationRepository.findByPostIdAndUserId(3L, 20L)).willReturn(Optional.of(mine));
        given(pointClient.refund(20L, 3L, null)).willReturn(new RefundResult(4 * 4840L, 30000L));

        LeaveResponse response = service.leave(20L, 3L, LeaveReason.CHANGED_MIND);

        assertThat(response).isEqualTo(
                new LeaveResponse(4 * 4840L, 30000L, PostStatus.RECRUITING, LeaveReason.CHANGED_MIND));
        assertThat(mine.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(mine.getLeaveReason()).isEqualTo(LeaveReason.CHANGED_MIND);
        assertThat(post.getFilledUnits()).isEqualTo(2);
        verify(chatClient).leave(3L, 20L);
    }

    @Test
    void 나눔_장소_의견이_맞지_않아_나가면_전액_환급하고_빈자리를_다시_모집한다() {
        post.addParticipant(4);
        post.markRecruited(NOW);
        post.proposeDistributionPlace("정문 편의점 앞", null, null, NOW);
        Participation mine = Participation.participant(post, member, 4, NOW);
        given(participationRepository.findByPostIdAndUserId(3L, 20L)).willReturn(Optional.of(mine));
        given(pointClient.refund(20L, 3L, null)).willReturn(new RefundResult(4 * 4840L, 30000L));

        LeaveResponse response = service.leave(20L, 3L, LeaveReason.PLACE_DISAGREEMENT);

        assertThat(response.refundedPoint()).isEqualTo(4 * 4840L);
        assertThat(response.postStatus()).isEqualTo(PostStatus.RECRUITING);
        assertThat(mine.getLeaveReason()).isEqualTo(LeaveReason.PLACE_DISAGREEMENT);
        assertThat(mine.isPlaceAgreed()).isFalse();
        assertThat(post.remainingUnits()).isEqualTo(4);
        verify(chatClient).sendSystemMessage(eq(3L), contains("나눔 장소 의견이 맞지 않아"));
    }

    @Test
    void 장소_제안_전에는_장소_불일치로_나갈_수_없다() {
        post.addParticipant(4);
        given(participationRepository.findByPostIdAndUserId(3L, 20L))
                .willReturn(Optional.of(Participation.participant(post, member, 4, NOW)));

        assertThatThrownBy(() -> service.leave(20L, 3L, LeaveReason.PLACE_DISAGREEMENT))
                .extracting("errorStatus").isEqualTo(ErrorStatus.DISTRIBUTION_PLACE_NOT_PROPOSED);
        verify(pointClient, never()).refund(any(), any(), any());
    }

    @Test
    void 나갔다가_다시_참여하면_장소_동의를_다시_받는다() {
        post.proposeDistributionPlace("정문", null, null, NOW);
        Participation old = Participation.participant(post, member, 1, NOW);
        old.agreePlace();
        old.leave(LeaveReason.PLACE_DISAGREEMENT, NOW);
        given(participationRepository.findByPostIdAndUserId(3L, 20L)).willReturn(Optional.of(old));
        given(pointClient.pay(any(), any(), any(), anyLong())).willReturn(new PaymentResult(4840L, 0L));

        service.join(20L, 3L, 1);

        assertThat(old.isActive()).isTrue();
        assertThat(old.isPlaceAgreed()).isFalse();
        assertThat(old.getLeaveReason()).isNull();
    }

    @Test
    void 호스트는_참여_취소할_수_없다() {
        given(participationRepository.findByPostIdAndUserId(3L, 10L))
                .willReturn(Optional.of(Participation.host(post, host, 2, NOW)));

        assertThatThrownBy(() -> service.leave(10L, 3L, LeaveReason.CHANGED_MIND))
                .extracting("errorStatus").isEqualTo(ErrorStatus.HOST_CANNOT_LEAVE);
    }

    @Test
    void 참여하지_않았으면_취소할_수_없다() {
        assertThatThrownBy(() -> service.leave(20L, 3L, LeaveReason.CHANGED_MIND))
                .extracting("errorStatus").isEqualTo(ErrorStatus.NOT_PARTICIPANT);
    }

    @Test
    void 주문완료_이후에는_참여_취소할_수_없다() {
        post.addParticipant(4);
        post.markRecruited(NOW);
        post.markOrdered(NOW);
        given(participationRepository.findByPostIdAndUserId(3L, 20L))
                .willReturn(Optional.of(Participation.participant(post, member, 4, NOW)));

        assertThatThrownBy(() -> service.leave(20L, 3L, LeaveReason.CHANGED_MIND))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_POST_STATUS);
        verify(pointClient, never()).refund(any(), any(), any());
    }

    @Test
    void 현황은_참여자에게만_보이고_받은_사람을_센다() {
        Participation hostPa = Participation.host(post, host, 2, NOW);
        Participation memberPa = Participation.participant(post, member, 1, NOW);
        hostPa.confirmReceipt(NOW);
        given(postRepository.existsById(3L)).willReturn(true);
        given(participationRepository.findActiveByPostId(3L)).willReturn(List.of(hostPa, memberPa));

        ParticipationStatusResponse status = service.getStatus(20L, 3L);

        assertThat(status.totalCount()).isEqualTo(2);
        assertThat(status.receivedCount()).isEqualTo(1);
        assertThat(status.participants().get(1).isMe()).isTrue();
        assertThat(status.participants().get(0).paidPoint()).isEqualTo(2 * 4840L);

        assertThatThrownBy(() -> service.getStatus(99L, 3L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.NOT_PARTICIPANT);
    }
}
