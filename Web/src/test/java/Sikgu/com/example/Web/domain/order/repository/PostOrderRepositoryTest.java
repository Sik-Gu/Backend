package Sikgu.com.example.Web.domain.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import Sikgu.com.example.Web.domain.order.entity.PostOrder;
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
class PostOrderRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);

    @Autowired
    PostOrderRepository orderRepository;
    @Autowired
    EntityManager em;

    @Test
    void 영수증_사진과_함께_저장하고_조회한다() {
        Post post = savePost(PostStatus.ORDERED);
        orderRepository.save(PostOrder.register(post, List.of("/r1.jpg", "/r2.jpg"), NOW));
        em.flush();
        em.clear();

        PostOrder found = orderRepository.findByPostId(post.getId()).orElseThrow();

        assertThat(found.receiptImageUrls()).containsExactly("/r1.jpg", "/r2.jpg");
        assertThat(orderRepository.existsByPostId(post.getId())).isTrue();
    }

    private Post savePost(PostStatus status) {
        User host = Fixtures.newUser("host" + System.nanoTime() % 100000);
        em.persist(host);
        Post post = Fixtures.post(host, NOW.plusDays(1));
        ReflectionTestUtils.setField(post, "status", status);
        em.persist(post);
        return post;
    }
}
