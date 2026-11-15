package Sikgu.com.example.Web.domain.inquiry.repository;

import Sikgu.com.example.Web.domain.inquiry.entity.ReceiptInquiry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceiptInquiryRepository extends JpaRepository<ReceiptInquiry, Long> {

    /** 공구의 모든 문의 (호스트용), 최신순 */
    @Query("""
            select distinct i from ReceiptInquiry i
            join fetch i.participation pa join fetch pa.user
            left join fetch i.imageUrls
            where pa.post.id = :postId
            order by i.createdAt desc
            """)
    List<ReceiptInquiry> findByPostId(@Param("postId") Long postId);

    /** 내 문의 (참여자용), 최신순 */
    @Query("""
            select distinct i from ReceiptInquiry i
            join fetch i.participation pa join fetch pa.user
            left join fetch i.imageUrls
            where pa.post.id = :postId and pa.user.id = :userId
            order by i.createdAt desc
            """)
    List<ReceiptInquiry> findByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
}
