package Sikgu.com.example.Web.domain.participation.repository;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    Optional<Participation> findByPostIdAndUserId(Long postId, Long userId);

    /** 참여 중(PAID)인 사람들. 호스트 먼저, 그다음 참여 순 */
    @Query("""
            select pa from Participation pa join fetch pa.user
            where pa.post.id = :postId
              and pa.paymentStatus = Sikgu.com.example.Web.domain.participation.enums.PaymentStatus.PAID
            order by case when pa.role = Sikgu.com.example.Web.domain.participation.enums.ParticipationRole.HOST
                          then 0 else 1 end, pa.joinedAt
            """)
    List<Participation> findActiveByPostId(@Param("postId") Long postId);

    /**
     * 사용자별 정산완료 공구 수를 카테고리별로 센다 (프로필의 주 거래 카테고리).
     * 결과: [userId, category, count]
     */
    @Query("""
            select pa.user.id, p.category, count(pa) from Participation pa join pa.post p
            where pa.user.id in :userIds
              and p.status = Sikgu.com.example.Web.domain.post.enums.PostStatus.SETTLED
              and pa.paymentStatus = Sikgu.com.example.Web.domain.participation.enums.PaymentStatus.PAID
            group by pa.user.id, p.category
            """)
    List<Object[]> countSettledByUserAndCategory(@Param("userIds") Collection<Long> userIds);
}
