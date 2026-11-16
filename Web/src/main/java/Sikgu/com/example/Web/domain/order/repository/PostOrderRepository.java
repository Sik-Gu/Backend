package Sikgu.com.example.Web.domain.order.repository;

import Sikgu.com.example.Web.domain.order.entity.PostOrder;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostOrderRepository extends JpaRepository<PostOrder, Long> {

    boolean existsByPostId(Long postId);

    @EntityGraph(attributePaths = "receiptImages")
    Optional<PostOrder> findByPostId(Long postId);

    /** 자동 수령 처리 대상: 나눔 진행 중이고 나눔 예정 시각이 기준 시각 이전인 공구 (D10) */
    @Query("""
            select o.post.id from PostOrder o
            where o.post.status = :status and o.distributeAt <= :before
            """)
    List<Long> findPostIdsToAutoConfirm(@Param("status") PostStatus status, @Param("before") LocalDateTime before);
}
