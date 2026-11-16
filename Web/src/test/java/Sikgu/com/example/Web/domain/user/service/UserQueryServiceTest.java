package Sikgu.com.example.Web.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import Sikgu.com.example.Web.domain.block.repository.BlockRepository;
import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.PaymentStatus;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.participation.service.TradeStatsReader;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.DistanceRange;
import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.review.enums.ReviewTag;
import Sikgu.com.example.Web.domain.review.repository.ReviewRepository;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.MeResponse;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.MyPostCard;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.ProfileResponse;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.enums.MyPostEnums.ActionRequired;
import Sikgu.com.example.Web.domain.user.enums.MyPostEnums.MyPostFilter;
import Sikgu.com.example.Web.domain.user.enums.MyPostEnums.MyPostTab;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.PageResponse;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.integration.point.PointClient;
import Sikgu.com.example.Web.integration.point.PointClient.PointSummary;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    UserRepository userRepository;
    @Mock
    ParticipationRepository participationRepository;
    @Mock
    ReviewRepository reviewRepository;
    @Mock
    BlockRepository blockRepository;
    @Mock
    TradeStatsReader tradeStatsReader;
    @Mock
    PointClient pointClient;

    UserQueryService service;
    User me = Fixtures.user(1L);
    User other = Fixtures.user(2L);

    // 내가 연 공구: 모집완료 + 나눔 장소 전원 동의 → 할 일 ORDER
    Post hostingRecruited;
    // 내가 참여한 공구: 나눔 진행 중 → 할 일 CONFIRM_RECEIPT
    Post joinedDistributed;
    // 내가 참여한 공구: 어제 정산 → 후기 안 씀 → WRITE_REVIEW
    Post joinedSettled;

    @BeforeEach
    void setUp() {
        service = new UserQueryService(userRepository, participationRepository, reviewRepository, blockRepository,
                tradeStatsReader, pointClient, CLOCK);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(userRepository.findById(2L)).willReturn(Optional.of(other));
        given(pointClient.getSummary(1L)).willReturn(new PointSummary(12000, 4840, 0));
        given(reviewRepository.findTargetIdsByWriterAndPost(anyLong(), any())).willReturn(List.of());

        hostingRecruited = post(10L, me, NOW.minusDays(3));
        hostingRecruited.addParticipant(2);
        hostingRecruited.addParticipant(4);
        hostingRecruited.markRecruited(NOW);
        hostingRecruited.proposeDistributionPlace("정문 앞", null, null, NOW);

        joinedDistributed = post(11L, other, NOW.minusDays(2));
        joinedDistributed.addParticipant(2);
        joinedDistributed.addParticipant(4);
        joinedDistributed.markRecruited(NOW);
        joinedDistributed.markOrdered(NOW);
        joinedDistributed.markShipping(NOW);
        joinedDistributed.markDistributed(NOW);

        joinedSettled = post(12L, other, NOW.minusDays(1));
        joinedSettled.addParticipant(2);
        joinedSettled.addParticipant(4);
        joinedSettled.markRecruited(NOW.minusDays(1));
        joinedSettled.markOrdered(NOW.minusDays(1));
        joinedSettled.markShipping(NOW.minusDays(1));
        joinedSettled.markDistributed(NOW.minusDays(1));
        joinedSettled.markSettled(NOW.minusDays(1));

        given(participationRepository.findMyPosts(1L)).willReturn(List.of(
                Participation.host(hostingRecruited, me, 2, NOW),
                Participation.participant(joinedDistributed, me, 4, NOW),
                Participation.participant(joinedSettled, me, 4, NOW)));
    }

    @Test
    void 내_정보는_공구_수와_포인트를_보여준다() {
        MeResponse response = service.getMe(1L);

        assertThat(response.postCounts().ongoing()).isEqualTo(2);
        assertThat(response.postCounts().done()).isEqualTo(1);
        assertThat(response.postCounts().actionRequired()).isEqualTo(3);
        assertThat(response.point().availableBalance()).isEqualTo(12000);
        assertThat(response.restricted()).isFalse();
    }

    @Test
    void 미수금이_있으면_제한_상태이고_할_일은_미수금_납부가_먼저다() {
        given(pointClient.getSummary(1L)).willReturn(new PointSummary(0, 0, 326));
        given(pointClient.hasUnpaidDebt(1L, 11L)).willReturn(true);

        assertThat(service.getMe(1L).restricted()).isTrue();
        PageResponse<MyPostCard> ongoing = service.getMyPosts(1L, MyPostTab.ONGOING, MyPostFilter.ALL, 0, 20);
        assertThat(ongoing.content()).filteredOn(card -> card.post().postId().equals(11L))
                .extracting(MyPostCard::actionRequired).containsExactly(ActionRequired.PAY_DEBT);
    }

    @Test
    void 진행중_탭은_최신순이고_역할에_맞는_할_일을_보여준다() {
        PageResponse<MyPostCard> page = service.getMyPosts(1L, MyPostTab.ONGOING, MyPostFilter.ALL, 0, 20);

        assertThat(page.content()).extracting(card -> card.post().postId()).containsExactly(11L, 10L);
        assertThat(page.content()).extracting(MyPostCard::actionRequired)
                .containsExactly(ActionRequired.CONFIRM_RECEIPT, ActionRequired.ORDER);
        assertThat(page.content().get(0).post().distance()).isEqualTo(DistanceRange.WITHIN_500M);
    }

    @Test
    void 호스트가_나눔_장소를_아직_제안하지_않았으면_PROPOSE_PLACE() {
        ReflectionTestUtils.setField(hostingRecruited, "distributionPlaceName", null);

        PageResponse<MyPostCard> page = service.getMyPosts(1L, MyPostTab.ONGOING, MyPostFilter.HOST, 0, 20);

        assertThat(page.content()).extracting(MyPostCard::actionRequired)
                .containsExactly(ActionRequired.PROPOSE_PLACE);
    }

    @Test
    void 장소에_동의하지_않은_참여자가_있으면_호스트는_기다린다() {
        given(participationRepository.countByPostIdAndPaymentStatusAndPlaceAgreed(10L, PaymentStatus.PAID, false))
                .willReturn(1L);

        PageResponse<MyPostCard> page = service.getMyPosts(1L, MyPostTab.ONGOING, MyPostFilter.HOST, 0, 20);

        assertThat(page.content()).extracting(MyPostCard::actionRequired).containsOnlyNulls();
    }

    @Test
    void 참여자는_제안된_나눔_장소에_동의하지_않았으면_AGREE_PLACE() {
        Post recruiting = post(13L, other, NOW.minusHours(1));
        recruiting.addParticipant(2);
        recruiting.addParticipant(1);
        recruiting.proposeDistributionPlace("후문", null, null, NOW);
        given(participationRepository.findMyPosts(1L)).willReturn(List.of(
                Participation.participant(recruiting, me, 1, NOW)));

        PageResponse<MyPostCard> page = service.getMyPosts(1L, MyPostTab.ONGOING, MyPostFilter.ALL, 0, 20);

        assertThat(page.content()).extracting(MyPostCard::actionRequired)
                .containsExactly(ActionRequired.AGREE_PLACE);
    }

    @Test
    void 완료_탭은_후기_진행을_보여주고_3일_안이면_후기_작성이_할_일() {
        PageResponse<MyPostCard> done = service.getMyPosts(1L, MyPostTab.DONE, MyPostFilter.ALL, 0, 20);

        assertThat(done.content()).hasSize(1);
        assertThat(done.content().get(0).reviewProgress().total()).isEqualTo(1);
        assertThat(done.content().get(0).reviewProgress().written()).isZero();
        assertThat(done.content().get(0).actionRequired()).isEqualTo(ActionRequired.WRITE_REVIEW);
    }

    @Test
    void 필터로_내가_연_공구와_할_일이_있는_공구만_본다() {
        assertThat(service.getMyPosts(1L, MyPostTab.ONGOING, MyPostFilter.HOST, 0, 20).content())
                .extracting(card -> card.post().postId()).containsExactly(10L);
        assertThat(service.getMyPosts(1L, MyPostTab.ONGOING, MyPostFilter.JOINED, 0, 20).content())
                .extracting(card -> card.post().postId()).containsExactly(11L);
        assertThat(service.getMyPosts(1L, MyPostTab.ONGOING, MyPostFilter.ACTION_REQUIRED, 0, 1).totalElements())
                .isEqualTo(2);
    }

    @Test
    void 페이지를_나눈다() {
        PageResponse<MyPostCard> first = service.getMyPosts(1L, MyPostTab.ONGOING, MyPostFilter.ALL, 0, 1);

        assertThat(first.content()).hasSize(1);
        assertThat(first.hasNext()).isTrue();
    }

    @Test
    void 프로필은_동_이름만_보여주고_차단_여부를_알려준다() {
        given(tradeStatsReader.mainCategories(List.of(2L))).willReturn(Map.of(2L, List.of(Category.FOOD)));
        given(reviewRepository.countTags(2L)).willReturn(List.<Object[]>of(new Object[] {ReviewTag.PUNCTUAL, 3L}));
        given(reviewRepository.findTop3ByTargetIdOrderByCreatedAtDesc(2L)).willReturn(List.of());
        given(participationRepository.findTop3ByUserIdAndPaymentStatusOrderByJoinedAtDesc(any(), any()))
                .willReturn(List.of());
        given(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).willReturn(true);

        ProfileResponse profile = service.getProfile(1L, 2L);

        assertThat(profile.dongName()).isEqualTo("역삼동");
        assertThat(profile.mainCategories()).containsExactly(Category.FOOD);
        assertThat(profile.tagCounts().get(0).count()).isEqualTo(3L);
        assertThat(profile.isBlocked()).isTrue();
    }

    @Test
    void 없는_사용자면_COMMON404() {
        given(userRepository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(1L, 9L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.USER_NOT_FOUND);
    }

    private static Post post(Long id, User host, LocalDateTime createdAt) {
        Post post = Fixtures.post(id, host, NOW.plusDays(5));
        ReflectionTestUtils.setField(post, "createdAt", createdAt);
        return post;
    }
}
