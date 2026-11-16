package Sikgu.com.example.Web.domain.block.repository;

import Sikgu.com.example.Web.domain.block.entity.Block;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlockRepository extends JpaRepository<Block, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /** 둘 중 한 명이라도 상대를 차단했는지 (차단은 양방향으로 작동) */
    @Query("""
            select count(b) > 0 from Block b
            where (b.blocker.id = :a and b.blocked.id = :b) or (b.blocker.id = :b and b.blocked.id = :a)
            """)
    boolean existsBetween(@Param("a") Long userA, @Param("b") Long userB);

    @EntityGraph(attributePaths = "blocked")
    Page<Block> findByBlockerIdOrderByCreatedAtDesc(Long blockerId, Pageable pageable);
}
