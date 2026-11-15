package Sikgu.com.example.Web.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.ParticipationRole;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.participation.service.TradeStatsReader;
import Sikgu.com.example.Web.domain.post.dto.request.PostCreateRequest;
import Sikgu.com.example.Web.domain.post.dto.request.TradePlaceRequest;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCreateResponse;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostDetailResponse;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.integration.chat.ChatClient;
import Sikgu.com.example.Web.integration.point.PointClient;
import Sikgu.com.example.Web.integration.point.PointClient.PaymentResult;
import Sikgu.com.example.Web.support.Fixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), SEOUL);
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

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, participationRepository, userRepository, tradeStatsReader,
                pointClient, chatClient, CLOCK);
    }

    @Test
    void 공구를_등록하면_호스트_참여를_만들고_호스트_몫을_결제하고_채팅방을_연다() {
        given(userRepository.findById(10L)).willReturn(Optional.of(host));
        given(postRepository.save(any(Post.class))).willAnswer(inv -> {
            ReflectionTestUtils.setField((Object) inv.getArgument(0), "id", 3L);
            return inv.getArgument(0);
        });
        given(participationRepository.save(any(Participation.class))).willAnswer(inv -> {
            ReflectionTestUtils.setField((Object) inv.getArgument(0), "id", 21L);
            return inv.getArgument(0);
        });
        given(pointClient.pay(10L, 3L, 21L, 9680L)).willReturn(new PaymentResult(9680L, 2320L));
        given(chatClient.createRoom(eq(3L), eq(10L), any())).willReturn(7L);

        PostCreateResponse response = postService.create(10L, request(2, 29040, NOW.plusDays(2)));

        assertThat(response).isEqualTo(new PostCreateResponse(3L, 7L, 4840, 9680L, 2320L));
        ArgumentCaptor<Participation> captor = ArgumentCaptor.forClass(Participation.class);
        verify(participationRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(ParticipationRole.HOST);
        assertThat(captor.getValue().getPost().getFilledUnits()).isEqualTo(2);
        assertThat(captor.getValue().getPost().getParticipantCount()).isEqualTo(1);
        verify(pointClient).validateNoUnpaidDebt(10L);
    }

    @Test
    void 호스트_구매_단위가_총_단위_이상이면_POST4001() {
        assertThatThrownBy(() -> postService.create(10L, request(6, 29040, NOW.plusDays(2))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_HOST_UNITS);
    }

    @Test
    void 마감이_지금이거나_과거면_POST4002() {
        assertThatThrownBy(() -> postService.create(10L, request(2, 29040, NOW)))
                .extracting("errorStatus").isEqualTo(ErrorStatus.DEADLINE_PASSED);
    }

    @Test
    void 총_가격이_나누어떨어지지_않으면_POST4003() {
        assertThatThrownBy(() -> postService.create(10L, request(2, 29041, NOW.plusDays(2))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INDIVISIBLE_PRICE);
    }

    @Test
    void 미수금이_있으면_등록하지_않는다() {
        willThrow(new GeneralException(ErrorStatus.UNPAID_DEBT)).given(pointClient).validateNoUnpaidDebt(10L);

        assertThatThrownBy(() -> postService.create(10L, request(2, 29040, NOW.plusDays(2))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.UNPAID_DEBT);
        verify(postRepository, never()).save(any());
    }

    @Test
    void 잔액이_부족하면_결제에서_실패해_전체가_롤백된다() {
        given(userRepository.findById(10L)).willReturn(Optional.of(host));
        given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));
        given(participationRepository.save(any(Participation.class))).willAnswer(inv -> inv.getArgument(0));
        given(pointClient.pay(any(), any(), any(), anyLong()))
                .willThrow(new GeneralException(ErrorStatus.INSUFFICIENT_POINT));

        assertThatThrownBy(() -> postService.create(10L, request(2, 29040, NOW.plusDays(2))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INSUFFICIENT_POINT);
        verify(chatClient, never()).createRoom(any(), any(), any());
    }

    @Test
    void 상세는_조회수를_올리고_참여자에게만_채팅방_ID를_준다() {
        Post post = Fixtures.post(3L, host, NOW.plusDays(2));
        post.addParticipant(2);
        User viewer = Fixtures.user(20L);
        post.addParticipant(1);
        Participation hostPa = Participation.host(post, host, 2, NOW);
        Participation viewerPa = Participation.participant(post, viewer, 1, NOW);
        given(postRepository.findDetailById(3L)).willReturn(Optional.of(post));
        given(userRepository.findById(20L)).willReturn(Optional.of(viewer));
        given(participationRepository.findActiveByPostId(3L)).willReturn(List.of(hostPa, viewerPa));
        given(tradeStatsReader.mainCategories(List.of(10L, 20L)))
                .willReturn(Map.of(10L, List.of(Category.FOOD), 20L, List.of()));
        given(chatClient.findRoomId(3L)).willReturn(Optional.of(7L));

        PostDetailResponse detail = postService.getDetail(20L, 3L);

        verify(postRepository).increaseViewCount(3L);
        assertThat(detail.viewCount()).isEqualTo(1);
        assertThat(detail.me().units()).isEqualTo(1);
        assertThat(detail.chatRoomId()).isEqualTo(7L);
        assertThat(detail.maxSelectableUnits()).isEqualTo(3);
        assertThat(detail.host().mainCategories()).containsExactly(Category.FOOD);
        assertThat(detail.participants()).hasSize(2);
        assertThat(detail.distanceKm()).isNotNull();
    }

    @Test
    void 참여하지_않은_사람은_me와_채팅방_ID가_null() {
        Post post = Fixtures.post(3L, host, NOW.plusDays(2));
        given(postRepository.findDetailById(3L)).willReturn(Optional.of(post));
        given(userRepository.findById(30L)).willReturn(Optional.of(Fixtures.user(30L)));
        given(participationRepository.findActiveByPostId(3L)).willReturn(List.of());
        given(tradeStatsReader.mainCategories(List.of())).willReturn(Map.of());

        PostDetailResponse detail = postService.getDetail(30L, 3L);

        assertThat(detail.me()).isNull();
        assertThat(detail.chatRoomId()).isNull();
        verify(chatClient, never()).findRoomId(any());
    }

    @Test
    void 없는_공구면_COMMON404() {
        given(postRepository.findDetailById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getDetail(1L, 99L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.POST_NOT_FOUND);
    }

    private static PostCreateRequest request(int hostUnits, int totalPrice, LocalDateTime deadlineAt) {
        return new PostCreateRequest("코스트코 생수 같이 사요", "커클랜드 생수", null, Category.FOOD, null,
                List.of("/images/post/a.jpg"), "2L 생수 4병", 6, totalPrice, hostUnits, deadlineAt,
                new TradePlaceRequest("정문 앞", "역삼동", 37.5012, 127.0396));
    }
}
