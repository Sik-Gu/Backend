package Sikgu.com.example.Web.domain.block.repository;

import static org.assertj.core.api.Assertions.assertThat;

import Sikgu.com.example.Web.domain.block.entity.Block;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.support.Fixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

/** 실제 JPA 쿼리를 H2 메모리 DB로 확인한다 */
@DataJpaTest
class BlockRepositoryTest {

    @Autowired
    BlockRepository blockRepository;
    @Autowired
    EntityManager em;

    @Test
    void 한쪽만_차단해도_양방향으로_차단_관계다() {
        User a = save("aaaa");
        User b = save("bbbb");
        User c = save("cccc");
        blockRepository.save(Block.of(a, b));

        assertThat(blockRepository.existsBetween(a.getId(), b.getId())).isTrue();
        assertThat(blockRepository.existsBetween(b.getId(), a.getId())).isTrue();
        assertThat(blockRepository.existsBetween(a.getId(), c.getId())).isFalse();
    }

    @Test
    void 내가_차단한_사용자만_최신순으로_조회한다() {
        User me = save("meme");
        User x = save("xxxx");
        User y = save("yyyy");
        User other = save("oooo");
        blockRepository.save(Block.of(me, x));
        blockRepository.save(Block.of(me, y));
        blockRepository.save(Block.of(other, me)); // 나를 차단한 사람은 안 보임
        em.flush();

        var page = blockRepository.findByBlockerIdOrderByCreatedAtDesc(me.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(block -> block.getBlocked().getLoginId())
                .containsExactlyInAnyOrder("xxxx", "yyyy");
    }

    private User save(String loginId) {
        User user = Fixtures.newUser(loginId);
        em.persist(user);
        return user;
    }
}
