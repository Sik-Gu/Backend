package Sikgu.com.example.Web.domain.post.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.post.service.PostService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecruitExpirySchedulerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    PostRepository postRepository;
    @Mock
    PostService postService;

    @Test
    void 마감_지난_모집중_공구를_하나씩_취소하고_하나가_실패해도_계속한다() {
        given(postRepository.findIdsToExpire(PostStatus.RECRUITING, LocalDateTime.now(CLOCK)))
                .willReturn(List.of(1L, 2L));
        given(postService.expire(1L)).willThrow(new RuntimeException("boom"));

        new RecruitExpiryScheduler(postRepository, postService, CLOCK).run();

        verify(postService).expire(1L);
        verify(postService).expire(2L);
    }
}
