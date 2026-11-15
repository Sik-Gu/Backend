package Sikgu.com.example.Web.domain.post.repository;

import Sikgu.com.example.Web.domain.post.entity.Post;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long>, PostSearchRepository {

    /**
     * 게시글 행에 비관적 락(SELECT ... FOR UPDATE)을 잡고 조회한다.
     * 참여·참여 취소·공구 취소·수령 확인처럼 단위·상태를 바꾸는 곳에서 쓴다 (D3).
     * 락 순서는 항상 게시글 → 지갑 (D4).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :postId")
    Optional<Post> findByIdForUpdate(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"host", "images"})
    @Query("select p from Post p where p.id = :postId")
    Optional<Post> findDetailById(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"host", "images"})
    @Query("select distinct p from Post p where p.id in :ids")
    List<Post> findAllWithImagesByIdIn(@Param("ids") Collection<Long> ids);

    /** 조회수 +1. 동시에 여러 명이 봐도 빠지지 않게 DB에서 바로 더한다 */
    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
    void increaseViewCount(@Param("postId") Long postId);
}
