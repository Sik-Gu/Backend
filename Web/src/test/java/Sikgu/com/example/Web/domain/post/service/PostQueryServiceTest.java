package Sikgu.com.example.Web.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCard;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.DistanceRange;
import Sikgu.com.example.Web.domain.post.enums.PostSort;
import Sikgu.com.example.Web.domain.post.search.PostSearchKeyword;
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
    void 검색_순서를_지키고_거리는_구간으로만_알려준다() {
        Post a = Fixtures.post(10L, Fixtures.user(5L), NOW.plusDays(1));
        Post b = Fixtures.post(11L, Fixtures.user(5L), NOW.plusDays(1));
        given(userRepository.findById(1L)).willReturn(Optional.of(viewer));
        given(postRepository.search(any())).willReturn(new PageImpl<>(
                List.of(new PostSearchRow(11L, 320.0), new PostSearchRow(10L, 870.0)), PageRequest.of(0, 9), 2));
        // DB가 다른 순서로 돌려줘도 검색 순서를 지킨다
        given(postRepository.findAllWithImagesByIdIn(List.of(11L, 10L))).willReturn(List.of(a, b));

        PageResponse<PostCard> page = service.search(1L, "  생수  ", null, 1000, true, PostSort.LATEST, 0, 9);

        assertThat(page.content()).extracting(PostCard::postId).containsExactly(11L, 10L);
        assertThat(page.content()).extracting(PostCard::distance)
                .containsExactly(DistanceRange.WITHIN_500M, DistanceRange.WITHIN_1KM);
        assertThat(page.totalElements()).isEqualTo(2);

        ArgumentCaptor<PostSearchCondition> captor = ArgumentCaptor.forClass(PostSearchCondition.class);
        verify(postRepository).search(captor.capture());
        assertThat(captor.getValue().keyword().mode()).isEqualTo(PostSearchKeyword.Mode.EXACT_WORD);
        assertThat(captor.getValue().keyword().words()).containsExactly("생수");
        assertThat(captor.getValue().latitude()).isEqualTo(viewer.getLatitude());
        assertThat(captor.getValue().radiusMeters()).isEqualTo(1000);
    }

    @Test
    void 초성만_입력하면_초성_검색으로_넘긴다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(viewer));
        given(postRepository.search(any())).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 9), 0));
        given(postRepository.findAllWithImagesByIdIn(List.of())).willReturn(List.of());

        service.search(1L, "ㅅㅅ ㄱㄱ", null, 500, true, PostSort.LATEST, 0, 9);

        ArgumentCaptor<PostSearchCondition> captor = ArgumentCaptor.forClass(PostSearchCondition.class);
        verify(postRepository).search(captor.capture());
        assertThat(captor.getValue().keyword().mode()).isEqualTo(PostSearchKeyword.Mode.CHOSUNG);
        assertThat(captor.getValue().keyword().chosung()).isEqualTo("ㅅㅅㄱㄱ");
    }

    @Test
    void 완성형_글자와_초성이_섞이면_SEARCH4001() {
        given(userRepository.findById(1L)).willReturn(Optional.of(viewer));

        assertThatThrownBy(() -> service.search(1L, "생ㅅ", null, 1000, true, PostSort.LATEST, 0, 9))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_SEARCH_KEYWORD);
    }

    @Test
    void 빈_검색어는_조건에서_뺀다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(viewer));
        given(postRepository.search(any())).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 9), 0));
        given(postRepository.findAllWithImagesByIdIn(List.of())).willReturn(List.of());

        service.search(1L, "   ", null, 1000, true, PostSort.LATEST, 0, 9);

        ArgumentCaptor<PostSearchCondition> captor = ArgumentCaptor.forClass(PostSearchCondition.class);
        verify(postRepository).search(captor.capture());
        assertThat(captor.getValue().keyword()).isNull();
    }

    @Test
    void 반경이_500m_1km가_아니면_COMMON400() {
        assertThatThrownBy(() -> service.search(1L, null, null, 3, true, PostSort.LATEST, 0, 9))
                .extracting("errorStatus").isEqualTo(ErrorStatus.BAD_REQUEST);
    }

    @Test
    void 집_위치를_설정하지_않았으면_USER4001() {
        ReflectionTestUtils.setField(viewer, "latitude", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(viewer));

        assertThatThrownBy(() -> service.search(1L, null, null, 1000, true, PostSort.LATEST, 0, 9))
                .extracting("errorStatus").isEqualTo(ErrorStatus.LOCATION_REQUIRED);
    }
}
