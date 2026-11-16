package Sikgu.com.example.Web.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.ParticipationRole;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.support.Fixtures;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

/** 게시글·참여 엔티티 매핑과 JPQL을 H2로 확인한다 */
@DataJpaTest
class PostRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);

    @Autowired
    PostRepository postRepository;
    @Autowired
    ParticipationRepository participationRepository;
    @Autowired
    EntityManager em;

    @Test
    void 이미지를_순서대로_저장하고_상세_조회로_함께_가져온다() {
        User host = persist(Fixtures.newUser("host"));
        Post post = Fixtures.post(host, NOW.plusDays(2));
        post.replaceImages(List.of("/images/a.jpg", "/images/b.jpg"));
        postRepository.save(post);
        em.flush();
        em.clear();

        Post found = postRepository.findDetailById(post.getId()).orElseThrow();

        assertThat(found.imageUrls()).containsExactly("/images/a.jpg", "/images/b.jpg");
        assertThat(found.thumbnailUrl()).isEqualTo("/images/a.jpg");
        assertThat(found.getHost().getSchoolEmail()).isEqualTo("host@korea.ac.kr");
        assertThat(found.getUnitPrice()).isEqualTo(4840);
    }

    @Test
    void 조회수를_DB에서_1_올린다() {
        Post post = postRepository.save(Fixtures.post(persist(Fixtures.newUser("host")), NOW.plusDays(2)));
        em.flush();

        postRepository.increaseViewCount(post.getId());
        em.clear();

        assertThat(postRepository.findById(post.getId()).orElseThrow().getViewCount()).isEqualTo(1);
    }

    @Test
    void 비관적_락으로_조회할_수_있다() {
        Post post = postRepository.save(Fixtures.post(persist(Fixtures.newUser("host")), NOW.plusDays(2)));
        em.flush();
        em.clear();

        assertThat(postRepository.findByIdForUpdate(post.getId())).isPresent();
    }

    @Test
    void 참여_중인_사람만_호스트_먼저_조회한다() {
        User host = persist(Fixtures.newUser("host"));
        User a = persist(Fixtures.newUser("aaaa"));
        User b = persist(Fixtures.newUser("bbbb"));
        Post post = postRepository.save(Fixtures.post(host, NOW.plusDays(2)));
        participationRepository.save(Participation.participant(post, a, 1, NOW));
        participationRepository.save(Participation.host(post, host, 2, NOW.plusMinutes(1)));
        Participation cancelled = participationRepository.save(Participation.participant(post, b, 1, NOW));
        cancelled.markRefunded();
        em.flush();
        em.clear();

        List<Participation> active = participationRepository.findActiveByPostId(post.getId());

        assertThat(active).extracting(Participation::getRole)
                .containsExactly(ParticipationRole.HOST, ParticipationRole.PARTICIPANT);
        assertThat(active).extracting(pa -> pa.getUser().getSchoolEmail()).containsExactly("host@korea.ac.kr", "aaaa@korea.ac.kr");
    }

    @Test
    void 정산완료_공구만_카테고리별로_센다() {
        User user = persist(Fixtures.newUser("user"));
        User host = persist(Fixtures.newUser("host"));
        Post settled = postRepository.save(Fixtures.post(host, NOW.plusDays(2)));
        ReflectionTestUtils.setField(settled, "status", Sikgu.com.example.Web.domain.post.enums.PostStatus.SETTLED);
        Post ongoing = postRepository.save(Fixtures.post(host, NOW.plusDays(2)));
        participationRepository.save(Participation.participant(settled, user, 1, NOW));
        participationRepository.save(Participation.participant(ongoing, user, 1, NOW));
        em.flush();

        List<Object[]> rows = participationRepository.countSettledByUserAndCategory(List.of(user.getId()));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)[0]).isEqualTo(user.getId());
        assertThat(rows.get(0)[1]).isEqualTo(Category.FOOD);
        assertThat(((Number) rows.get(0)[2]).longValue()).isEqualTo(1L);
    }

    private User persist(User user) {
        em.persist(user);
        return user;
    }
}
