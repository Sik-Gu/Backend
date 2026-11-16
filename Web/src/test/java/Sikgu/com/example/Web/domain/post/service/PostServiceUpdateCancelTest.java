package Sikgu.com.example.Web.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.PaymentStatus;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.participation.service.TradeStatsReader;
import Sikgu.com.example.Web.domain.post.dto.request.PostUpdateRequest;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCancelResponse;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
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

@ExtendWith(MockitoExtension.class)
class PostServiceUpdateCancelTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    PostRepository postRepository;
    @Mock
    ParticipationRepository participationRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    TradeStatsReader tradeStatsReader;
    @Mock
    PointClient pointClient;
    @Mock
    ChatClient chatClient;

    PostService postService;
    User host = Fixtures.user(10L);
    User member = Fixtures.user(20L);
    Post post;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, participationRepository, userRepository, tradeStatsReader,
                pointClient, chatClient, CLOCK);
        post = Fixtures.post(3L, host, NOW.plusDays(2));
        post.addParticipant(2); // 호스트
    }

    // ---------- 수정 ----------

    @Test
    void 보낸_값만_바꾸고_이미지와_검색_색인도_바꾼다() {
        given(postRepository.findByIdForUpdate(3L)).willReturn(Optional.of(post));

        postService.update(10L, 3L, new PostUpdateRequest("새 제목", null, null, Category.DAILY, null,
                List.of("/images/new.jpg"), NOW.plusDays(5)));

        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getProductName()).isEqualTo("커클랜드 생수 2L x 24"); // 안 보낸 값은 그대로
        assertThat(post.getCategory()).isEqualTo(Category.DAILY);
        assertThat(post.imageUrls()).containsExactly("/images/new.jpg");
        assertThat(post.getDeadlineAt()).isEqualTo(NOW.plusDays(5));
        assertThat(post.getSearchWords()).contains(" 새 ", " 제목 ");
        assertThat(post.getTitleChosung()).isEqualTo("ㅅㅈㅁ");
        assertThat(post.getUnitPrice()).isEqualTo(4840); // 가격은 바뀌지 않는다
    }

    @Test
    void 호스트가_아니면_AUTH403() {
        given(postRepository.findByIdForUpdate(3L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(20L, 3L, emptyUpdate()))
                .extracting("errorStatus").isEqualTo(ErrorStatus.NOT_POST_HOST);
    }

    @Test
    void 다른_참여자가_있으면_POST4091() {
        post.addParticipant(1);
        given(postRepository.findByIdForUpdate(3L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(10L, 3L, emptyUpdate()))
                .extracting("errorStatus").isEqualTo(ErrorStatus.POST_NOT_EDITABLE);
    }

    @Test
    void 마감을_과거로_바꾸면_POST4002() {
        given(postRepository.findByIdForUpdate(3L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(10L, 3L,
                new PostUpdateRequest(null, null, null, null, null, null, NOW.minusMinutes(1))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.DEADLINE_PASSED);
    }

    // ---------- 취소 ----------

    @Test
    void 취소하면_호스트_포함_전원_환급하고_채팅방을_닫는다() {
        post.addParticipant(1);
        Participation hostPa = Participation.host(post, host, 2, NOW);
        Participation memberPa = Participation.participant(post, member, 1, NOW);
        given(postRepository.findByIdForUpdate(3L)).willReturn(Optional.of(post));
        given(participationRepository.findActiveByPostId(3L)).willReturn(List.of(hostPa, memberPa));

        PostCancelResponse response = postService.cancel(10L, 3L);

        assertThat(response).isEqualTo(new PostCancelResponse(3L, PostStatus.CANCELLED, 2, 3 * 4840L));
        assertThat(post.getCancelledAt()).isEqualTo(NOW);
        assertThat(hostPa.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(memberPa.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(pointClient).refund(10L, 3L, hostPa.getId());
        verify(pointClient).refund(20L, 3L, memberPa.getId());
        verify(chatClient).close(3L);
    }

    @Test
    void 주문완료_이후에는_취소할_수_없다() {
        post.addParticipant(4);
        post.markRecruited(NOW);
        post.markOrdered(NOW);
        given(postRepository.findByIdForUpdate(3L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.cancel(10L, 3L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.POST_NOT_CANCELLABLE);
        verify(pointClient, never()).refund(any(), any(), any());
    }

    @Test
    void 호스트가_아니면_취소할_수_없다() {
        given(postRepository.findByIdForUpdate(3L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.cancel(20L, 3L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.NOT_POST_HOST);
    }

    @Test
    void 없는_공구면_COMMON404() {
        given(postRepository.findByIdForUpdate(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.cancel(10L, 99L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.POST_NOT_FOUND);
    }

    private static PostUpdateRequest emptyUpdate() {
        return new PostUpdateRequest(null, null, null, null, null, null, null);
    }
}
