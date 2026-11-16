package Sikgu.com.example.Web.domain.review.repository;

import Sikgu.com.example.Web.domain.review.entity.Review;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 이 공구에서 내가 후기를 쓴 대상들 */
    @Query("select r.target.id from Review r where r.writer.id = :writerId and r.post.id = :postId")
    List<Long> findTargetIdsByWriterAndPost(@Param("writerId") Long writerId, @Param("postId") Long postId);

    /** 받은 후기 목록 (최신순) */
    @EntityGraph(attributePaths = {"writer", "post", "post.host"})
    Page<Review> findByTargetIdOrderByCreatedAtDesc(Long targetId, Pageable pageable);

    /** 받은 후기 최근 3건 (프로필) */
    @EntityGraph(attributePaths = {"writer", "post"})
    List<Review> findTop3ByTargetIdOrderByCreatedAtDesc(Long targetId);

    long countByTargetId(Long targetId);

    @Query("select avg(r.rating) from Review r where r.target.id = :targetId")
    Double averageRating(@Param("targetId") Long targetId);

    /** 받은 태그별 개수. 결과: [ReviewTag, count] */
    @Query("""
            select t, count(r) from Review r join r.tags t
            where r.target.id = :targetId
            group by t order by count(r) desc
            """)
    List<Object[]> countTags(@Param("targetId") Long targetId);
}
