package Sikgu.com.example.Web.domain.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.review.entity.Review;
import Sikgu.com.example.Web.domain.review.enums.ReviewTag;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.support.Fixtures;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired
    ReviewRepository repository;
    @Autowired
    EntityManager em;

    @Test
    void 받은_후기의_평균_개수_태그와_목록을_조회한다() {
        User host = persist(Fixtures.newUser("host"));
        User a = persist(Fixtures.newUser("aaaa"));
        User b = persist(Fixtures.newUser("bbbb"));
        Post post = Fixtures.post(host, LocalDateTime.of(2026, 11, 20, 12, 0));
        em.persist(post);
        repository.save(Review.write(a, post, host, 5, Set.of(ReviewTag.PUNCTUAL, ReviewTag.CLEAN_SPLIT), "최고"));
        repository.save(Review.write(b, post, host, 4, Set.of(ReviewTag.PUNCTUAL), "좋아요"));
        repository.save(Review.write(host, post, a, 3, Set.of(), null));
        em.flush();
        em.clear();

        assertThat(repository.averageRating(host.getId())).isEqualTo(4.5);
        assertThat(repository.countByTargetId(host.getId())).isEqualTo(2);
        List<Object[]> tags = repository.countTags(host.getId());
        assertThat(tags.get(0)[0]).isEqualTo(ReviewTag.PUNCTUAL);
        assertThat(((Number) tags.get(0)[1]).longValue()).isEqualTo(2L);
        assertThat(repository.findTargetIdsByWriterAndPost(a.getId(), post.getId())).containsExactly(host.getId());
        assertThat(repository.findByTargetIdOrderByCreatedAtDesc(host.getId(), PageRequest.of(0, 10))
                .getContent()).hasSize(2).allSatisfy(r -> assertThat(r.getTags()).isNotEmpty());
        assertThat(repository.findTop3ByTargetIdOrderByCreatedAtDesc(a.getId())).hasSize(1);
    }

    private User persist(User user) {
        em.persist(user);
        return user;
    }
}
