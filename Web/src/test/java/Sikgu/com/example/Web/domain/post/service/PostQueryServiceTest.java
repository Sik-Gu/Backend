package Sikgu.com.example.Web.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCard;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.PostSort;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.post.repository.PostSearchCondition;
import Sikgu.com.example.Web.domain.post.repository.PostSearchRepository.PostSearchRow;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.PageResponse;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    PostRepository postRepository;
    @Mock
    UserRepository userRepository;

    PostQueryService service;
    User viewer = Fixtures.user(1L);

    @BeforeEach
    void setUp() {
        service = new PostQueryService(postRepository, userRepository, CLOCK);
    }

    @Test
    void 검색_순서를_지키고_거리를_반올림한다() {
        Post a = Fixtures.post(10L, Fixtures.user(5L), NOW.plusDays(1));
        Post b = Fixtures.post(11L, Fixtures.user(5L), NOW.plusDays(1));
        given(userRepository.findById(1L)).willReturn(Optional.of(viewer));
        given(postRepository.search(any())).willReturn(new PageImpl<>(
                List.of(new PostSearchRow(11L, 0.84), new PostSearchRow(10L, 2.06)), PageRequest.of(0, 9), 2));
        // DB가 다른 순서로 돌려줘도 검색 순서를 지킨다
        given(postRepository.findAllWithImagesByIdIn(List.of(11L, 10L))).willReturn(List.of(a, b));

        PageResponse<PostCard> page = service.search(1L, "  생수  ", null, 3, true, PostSort.LATEST, 0, 9);

        assertThat(page.content()).extracting(PostCard::postId).containsExactly(11L, 10L);
        assertThat(page.content()).extracting(PostCard::distanceKm).containsExactly(0.8, 2.1);
        assertThat(page.totalElements()).isEqualTo(2);

        ArgumentCaptor<PostSearchCondition> captor = ArgumentCaptor.forClass(PostSearchCondition.class);
        verify(postRepository).search(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("생수");
        assertThat(captor.getValue().latitude()).isEqualTo(viewer.getLatitude());
    }

    @Test
    void 빈_검색어는_조건에서_뺀다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(viewer));
        given(postRepository.search(any())).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 9), 0));
        given(postRepository.findAllWithImagesByIdIn(List.of())).willReturn(List.of());

        service.search(1L, "   ", null, 3, true, PostSort.LATEST, 0, 9);

        ArgumentCaptor<PostSearchCondition> captor = ArgumentCaptor.forClass(PostSearchCondition.class);
        verify(postRepository).search(captor.capture());
        assertThat(captor.getValue().keyword()).isNull();
    }

    @Test
    void 반경이_1_3_5가_아니면_COMMON400() {
        assertThatThrownBy(() -> service.search(1L, null, null, 2, true, PostSort.LATEST, 0, 9))
                .extracting("errorStatus").isEqualTo(ErrorStatus.BAD_REQUEST);
    }

    @Test
    void 동네가_없으면_USER4001() {
        ReflectionTestUtils.setField(viewer, "latitude", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(viewer));

        assertThatThrownBy(() -> service.search(1L, null, null, 3, true, PostSort.LATEST, 0, 9))
                .extracting("errorStatus").isEqualTo(ErrorStatus.LOCATION_REQUIRED);
    }
}
