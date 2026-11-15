package com.sikgu.web.domain.post.repository;

import com.sikgu.web.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 조회수 +1 (DB에서 바로 더함)
    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
    int increaseViewCount(@Param("postId") Long postId);

    // 게시글 + 호스트를 쿼리 한 번에
    @Query("select p from Post p join fetch p.host where p.id = :postId")
    Optional<Post> findWithHostById(@Param("postId") Long postId);
}