package Sikgu.com.example.Web.domain.participation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.PaymentStatus;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.support.Fixtures;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class ParticipationRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);

    @Autowired
    ParticipationRepository repository;
    @Autowired
    EntityManager em;

    @Test
    void 내_공구함은_참여_중인_공구와_취소된_공구만_보여준다() {
        User me = persist(Fixtures.newUser("meme"));
        User host = persist(Fixtures.newUser("host"));
        Post ongoing = persistPost(host, PostStatus.RECRUITING);
        Post cancelledByHost = persistPost(host, PostStatus.CANCELLED);
        Post leftByMe = persistPost(host, PostStatus.RECRUITING);
        Post mine = persistPost(me, PostStatus.RECRUITING);
        repository.save(Participation.participant(ongoing, me, 1, NOW));
        repository.save(Participation.participant(cancelledByHost, me, 1, NOW)).markRefunded();
        repository.save(Participation.participant(leftByMe, me, 1, NOW)).markRefunded();
        repository.save(Participation.host(mine, me, 2, NOW));
        em.flush();
        em.clear();

        List<Participation> result = repository.findMyPosts(me.getId());

        assertThat(result).extracting(pa -> pa.getPost().getId())
                .containsExactlyInAnyOrder(ongoing.getId(), cancelledByHost.getId(), mine.getId());
    }

    @Test
    void 최근_거래는_참여_중인_것만_최신순_3건() {
        User me = persist(Fixtures.newUser("meme"));
        User host = persist(Fixtures.newUser("host"));
        for (int i = 0; i < 4; i++) {
            repository.save(Participation.participant(persistPost(host, PostStatus.SETTLED), me, 1, NOW.plusDays(i)));
        }
        em.flush();
        em.clear();

        List<Participation> recent = repository.findTop3ByUserIdAndPaymentStatusOrderByJoinedAtDesc(
                me.getId(), PaymentStatus.PAID);

        assertThat(recent).hasSize(3);
        assertThat(recent.get(0).getJoinedAt()).isEqualTo(NOW.plusDays(3));
        assertThat(recent.get(0).getPost().getTitle()).isNotBlank();
    }

    private Post persistPost(User host, PostStatus status) {
        Post post = Fixtures.post(host, NOW.plusDays(1));
        ReflectionTestUtils.setField(post, "status", status);
        em.persist(post);
        return post;
    }

    private User persist(User user) {
        em.persist(user);
        return user;
    }
}
