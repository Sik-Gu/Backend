package Sikgu.com.example.Web.domain.order.repository;

import Sikgu.com.example.Web.domain.order.entity.PostOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostOrderRepository extends JpaRepository<PostOrder, Long> {

    boolean existsByPostId(Long postId);

    @EntityGraph(attributePaths = "receiptImages")
    Optional<PostOrder> findByPostId(Long postId);
}
