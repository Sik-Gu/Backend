package Sikgu.com.example.Web.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.ParticipationRole;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.review.dto.request.ReviewWriteRequest;
import Sikgu.com.example.Web.domain.review.dto.request.ReviewWriteRequest.ReviewItem;
import Sikgu.com.example.Web.domain.review.dto.response.ReviewResponses.ReviewTarget;
import Sikgu.com.example.Web.domain.review.dto.response.ReviewResponses.ReviewTargetsResponse;
import Sikgu.com.example.Web.domain.review.enums.ReviewTag;
import Sikgu.com.example.Web.domain.review.repository.ReviewRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.support.Fixtures;
import java.math.BigDecimal;
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
import org.springframework.dao.DataIntegrityViolationException;

/** 정산 시각 = 기준 시각 1일 전 (작성 기간 안) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    PostRepository postRepository;
    @Mock
    ParticipationRepository participationRepository;
    @Mock
    ReviewRepository reviewRepository;
    @Mock
    UserRepository userRepository;

    ReviewService service;
    User host = Fixtures.user(10L);
    User a = Fixtures.user(20L);
    User b = Fixtures.user(30L);
    Post post;

    @BeforeEach
    void setUp() {
        service = new ReviewService(postRepository, participationRepository, reviewRepository, userRepository, CLOCK);
        post = settledPost(NOW.minusDays(1));
        given(postRepository.findById(3L)).willReturn(Optional.of(post));
        given(participationRepository.findActiveByPostId(3L)).willReturn(List.of(
                Participation.host(post, host, 2, NOW), Participation.participant(post, a, 1, NOW),
                Participation.participant(post, b, 3, NOW)));
        given(reviewRepository.findTargetIdsByWriterAndPost(any(), any())).willReturn(List.of());
        given(reviewRepository.averageRating(any())).willReturn(4.0);
    }

    @Test
    void 호스트의_평가_대상은_참여자_전원() {
        given(reviewRepository.findTargetIdsByWriterAndPost(10L, 3L)).willReturn(List.of(20L));

        ReviewTargetsResponse response = service.getTargets(10L, 3L);

        assertThat(response.myRole()).isEqualTo(ParticipationRole.HOST);
        assertThat(response.targets()).extracting(ReviewTarget::userId).containsExactly(20L, 30L);
        assertThat(response.targets()).extracting(ReviewTarget::written).containsExactly(true, false);
        assertThat(response.writableUntil()).isEqualTo(NOW.minusDays(1).plusDays(3));
        assertThat(response.canWrite()).isTrue();
    }

    @Test
    void 참여자의_평가_대상은_호스트_1명() {
        ReviewTargetsResponse response = service.getTargets(20L, 3L);

        assertThat(response.targets()).extracting(ReviewTarget::userId).containsExactly(10L);
    }

    @Test
    void 후기를_쓰면_대상의_평균_별점을_갱신한다() {
        given(reviewRepository.averageRating(10L)).willReturn(4.25);

        service.write(20L, 3L, request(item(10L, 4)));

        verify(reviewRepository).saveAllAndFlush(any());
        assertThat(host.getRatingAvg()).isEqualTo(new BigDecimal("4.3"));
    }

    @Test
    void 정산_후_3일이_지나면_REVIEW4092() {
        Post old = settledPost(NOW.minusDays(3).minusMinutes(1));
        given(postRepository.findById(3L)).willReturn(Optional.of(old));

        assertThatThrownBy(() -> service.write(20L, 3L, request(item(10L, 4))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.REVIEW_PERIOD_CLOSED);
    }

    @Test
    void 정산_전이면_REVIEW4092() {
        Post distributing = Fixtures.post(3L, host, NOW.plusDays(1));
        given(postRepository.findById(3L)).willReturn(Optional.of(distributing));

        assertThatThrownBy(() -> service.write(20L, 3L, request(item(10L, 4))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.REVIEW_PERIOD_CLOSED);
    }

    @Test
    void 참여자끼리나_본인은_평가할_수_없다() {
        assertThatThrownBy(() -> service.write(20L, 3L, request(item(30L, 4))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.REVIEW_TARGET_INVALID);
        assertThatThrownBy(() -> service.write(20L, 3L, request(item(20L, 4))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.REVIEW_TARGET_INVALID);
        verify(reviewRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void 이미_쓴_대상이면_REVIEW4091() {
        given(reviewRepository.findTargetIdsByWriterAndPost(20L, 3L)).willReturn(List.of(10L));

        assertThatThrownBy(() -> service.write(20L, 3L, request(item(10L, 4))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.REVIEW_DUPLICATED);
    }

    @Test
    void 동시에_써서_UNIQUE에_걸려도_REVIEW4091() {
        given(reviewRepository.saveAllAndFlush(any())).willThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.write(20L, 3L, request(item(10L, 4))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.REVIEW_DUPLICATED);
    }

    @Test
    void 한_요청에_같은_대상이나_같은_태그가_두번이면_COMMON400() {
        assertThatThrownBy(() -> service.write(10L, 3L, request(item(20L, 5), item(20L, 4))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.BAD_REQUEST);
        assertThatThrownBy(() -> service.write(20L, 3L, request(new ReviewItem(10L, 5,
                List.of(ReviewTag.PUNCTUAL, ReviewTag.PUNCTUAL), null))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.BAD_REQUEST);
    }

    @Test
    void 참여하지_않은_사람은_쓸_수_없다() {
        assertThatThrownBy(() -> service.write(99L, 3L, request(item(10L, 4))))
                .extracting("errorStatus").isEqualTo(ErrorStatus.NOT_PARTICIPANT);
    }

    private Post settledPost(LocalDateTime settledAt) {
        Post p = Fixtures.post(3L, host, settledAt.plusDays(1));
        p.addParticipant(2);
        p.addParticipant(1);
        p.addParticipant(3);
        p.markRecruited(settledAt);
        p.markOrdered(settledAt);
        p.markShipping(settledAt);
        p.markDistributed(settledAt);
        p.markSettled(settledAt);
        return p;
    }

    private static ReviewItem item(Long targetId, int rating) {
        return new ReviewItem(targetId, rating, List.of(ReviewTag.PUNCTUAL), "좋았어요");
    }

    private static ReviewWriteRequest request(ReviewItem... items) {
        return new ReviewWriteRequest(List.of(items));
    }
}
