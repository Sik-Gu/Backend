package Sikgu.com.example.Web.domain.participation.service;

import static org.assertj.core.api.Assertions.assertThat;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.support.Fixtures;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * D3 동시성 확인: 남은 단위보다 많은 사람이 동시에 참여해도 총 단위를 넘지 않는다.
 * 실제 서비스와 H2(메모리 DB)로 게시글 행 비관적 락을 확인한다.
 */
@SpringBootTest(properties = "jwt.secret=test-secret-key-for-sikgu-jwt-must-be-32-bytes-or-more")
class ParticipationConcurrencyTest {

    @Autowired
    ParticipationService participationService;
    @Autowired
    PostRepository postRepository;
    @Autowired
    ParticipationRepository participationRepository;
    @Autowired
    UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        participationRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 남은_4단위에_10명이_동시에_1단위씩_참여해도_4명만_성공한다() throws Exception {
        User host = userRepository.save(Fixtures.newUser("host"));
        Post post = Fixtures.post(host, LocalDateTime.now().plusDays(1));
        post.addParticipant(2); // 호스트 2단위 → 남은 4단위
        postRepository.save(post);
        participationRepository.save(Participation.host(post, host, 2, LocalDateTime.now()));

        List<User> members = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            members.add(userRepository.save(Fixtures.newUser("member" + i)));
        }

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(members.size());
        AtomicInteger success = new AtomicInteger();
        ConcurrentLinkedQueue<ErrorStatus> failures = new ConcurrentLinkedQueue<>();
        for (User member : members) {
            pool.submit(() -> {
                try {
                    start.await();
                    participationService.join(member.getId(), post.getId(), 1);
                    success.incrementAndGet();
                } catch (GeneralException e) {
                    failures.add(e.getErrorStatus());
                } catch (Exception e) {
                    failures.add(ErrorStatus.INTERNAL_SERVER_ERROR);
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        Post result = postRepository.findById(post.getId()).orElseThrow();
        assertThat(success.get()).isEqualTo(4);
        assertThat(result.getFilledUnits()).isEqualTo(6);
        assertThat(result.getParticipantCount()).isEqualTo(5);
        assertThat(result.getStatus()).isEqualTo(PostStatus.RECRUITED);
        // 늦게 온 6명은 모집이 끝나서(POST409) 또는 남은 단위가 없어서(PART4001) 실패
        assertThat(failures).hasSize(6)
                .allMatch(s -> s == ErrorStatus.INVALID_POST_STATUS || s == ErrorStatus.EXCEED_REMAINING_UNITS);
    }
}
