package Sikgu.com.example.Web.domain.participation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.dto.request.DistributionPlaceRequest;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.DistributionPlaceInfo;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.integration.chat.ChatClient;
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

/**
 * 나눔 장소 합의. 생수 6단위: 호스트 2단위, 참여자 A 1단위, 참여자 B 3단위 (모집완료)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DistributionPlaceServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    PostRepository postRepository;
    @Mock
    ParticipationRepository participationRepository;
    @Mock
    ChatClient chatClient;

    DistributionPlaceService service;
    User host = Fixtures.user(10L);
    User a = Fixtures.user(20L);
    User b = Fixtures.user(30L);
    Post post;
    Participation hostPa;
    Participation aPa;
    Participation bPa;

    @BeforeEach
    void setUp() {
        service = new DistributionPlaceService(postRepository, participationRepository, chatClient, CLOCK);
        post = Fixtures.post(3L, host, NOW.plusDays(2));
        post.addParticipant(2);
        post.addParticipant(1);
        post.addParticipant(3);
        post.markRecruited(NOW);
        hostPa = Participation.host(post, host, 2, NOW);
        aPa = Participation.participant(post, a, 1, NOW);
        bPa = Participation.participant(post, b, 3, NOW);
        given(postRepository.findByIdForUpdate(3L)).willReturn(Optional.of(post));
        given(participationRepository.findActiveByPostId(3L)).willReturn(List.of(hostPa, aPa, bPa));
    }

    @Test
    void 호스트가_장소를_제안하면_호스트만_동의한_상태로_시작한다() {
        DistributionPlaceInfo info = service.propose(10L, 3L,
                new DistributionPlaceRequest("정문 편의점 앞", 37.5010, 127.0370));

        assertThat(post.getDistributionPlaceName()).isEqualTo("정문 편의점 앞");
        assertThat(info.agreedCount()).isEqualTo(1);
        assertThat(info.memberCount()).isEqualTo(3);
        assertThat(info.allAgreed()).isFalse();
        assertThat(info.myAgreed()).isTrue();
        verify(chatClient).sendSystemMessage(eq(3L), contains("정문 편의점 앞"));
    }

    @Test
    void 장소를_바꾸면_참여자_동의가_초기화된다() {
        service.propose(10L, 3L, new DistributionPlaceRequest("정문", null, null));
        aPa.agreePlace();
        bPa.agreePlace();

        DistributionPlaceInfo info = service.propose(10L, 3L, new DistributionPlaceRequest("후문", null, null));

        assertThat(info.name()).isEqualTo("후문");
        assertThat(aPa.isPlaceAgreed()).isFalse();
        assertThat(bPa.isPlaceAgreed()).isFalse();
        assertThat(hostPa.isPlaceAgreed()).isTrue();
    }

    @Test
    void 호스트가_아니면_장소를_제안할_수_없다() {
        assertThatThrownBy(() -> service.propose(20L, 3L, new DistributionPlaceRequest("정문", null, null)))
                .extracting("errorStatus").isEqualTo(ErrorStatus.NOT_POST_HOST);
    }

    @Test
    void 주문_이후에는_장소를_바꿀_수_없다() {
        post.markOrdered(NOW);

        assertThatThrownBy(() -> service.propose(10L, 3L, new DistributionPlaceRequest("정문", null, null)))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_POST_STATUS);
    }

    @Test
    void 전원이_동의하면_allAgreed이고_주문_안내_메시지를_보낸다() {
        service.propose(10L, 3L, new DistributionPlaceRequest("정문", null, null));

        service.agree(20L, 3L);
        verify(chatClient, never()).sendSystemMessage(eq(3L), contains("모두 나눔 장소에 동의"));
        DistributionPlaceInfo info = service.agree(30L, 3L);

        assertThat(info.allAgreed()).isTrue();
        assertThat(info.agreedCount()).isEqualTo(3);
        verify(chatClient).sendSystemMessage(eq(3L), contains("모두 나눔 장소에 동의"));
    }

    @Test
    void 장소_제안_전에는_동의할_수_없다() {
        assertThatThrownBy(() -> service.agree(20L, 3L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.DISTRIBUTION_PLACE_NOT_PROPOSED);
    }

    @Test
    void 이미_동의했으면_PLACE4093() {
        service.propose(10L, 3L, new DistributionPlaceRequest("정문", null, null));
        service.agree(20L, 3L);

        assertThatThrownBy(() -> service.agree(20L, 3L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.PLACE_ALREADY_AGREED);
    }

    @Test
    void 참여자가_아니면_동의할_수_없다() {
        service.propose(10L, 3L, new DistributionPlaceRequest("정문", null, null));

        assertThatThrownBy(() -> service.agree(99L, 3L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.NOT_PARTICIPANT);
    }

    @Test
    void 좌표는_둘_다_보내거나_둘_다_비워야_한다() {
        assertThat(new DistributionPlaceRequest("정문", null, null).isCoordinateBothOrNone()).isTrue();
        assertThat(new DistributionPlaceRequest("정문", 37.5, 127.0).isCoordinateBothOrNone()).isTrue();
        assertThat(new DistributionPlaceRequest("정문", 37.5, null).isCoordinateBothOrNone()).isFalse();
    }
}
