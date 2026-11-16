package Sikgu.com.example.Web.domain.post.scheduler;

import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.post.service.PostService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 모집 기간(등록 + 7일)이 지났는데 모집완료가 안 된 공구를 자동 취소하고 전원 환급한다 (D24).
 * 한 공구가 실패해도 나머지는 계속 처리한다 (공구마다 별도 트랜잭션).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitExpiryScheduler {

    private final PostRepository postRepository;
    private final PostService postService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${post.expire.interval:PT1M}", initialDelayString = "${post.expire.initial-delay:PT30S}")
    public void run() {
        List<Long> postIds = postRepository.findIdsToExpire(PostStatus.RECRUITING, LocalDateTime.now(clock));
        for (Long postId : postIds) {
            try {
                if (postService.expire(postId)) {
                    log.info("모집 기간 만료로 공구 자동 취소 post={}", postId);
                }
            } catch (RuntimeException e) {
                log.error("공구 자동 취소 실패 post={}", postId, e);
            }
        }
    }
}
