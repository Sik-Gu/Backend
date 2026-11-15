package Sikgu.com.example.Web.domain.inquiry.entity;

import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.DesiredResolution;
import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.InquiryResolution;
import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.InquiryStatus;
import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.InquiryType;
import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.user.entity.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 수령 문의 (docs/DB.md 4.7 receipt_inquiries). 운영팀이 처리한다 (D10).
 * 처리 컬럼(resolution, refundAmount, admin, adminMemo, resolvedAt)은 관리자 기능이 생기면 채운다.
 */
@Entity
@Table(name = "receipt_inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReceiptInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    private Participation participation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InquiryType type;

    /** 실제로 받은 단위 (수량 부족일 때) */
    private Integer receivedUnits;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DesiredResolution desiredResolution;

    @Column(nullable = false, length = 1000)
    private String content;

    @ElementCollection
    @CollectionTable(name = "inquiry_images", joinColumns = @JoinColumn(name = "inquiry_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "image_url", nullable = false, length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private InquiryResolution resolution;

    private Integer refundAmount;

    /** 처리한 운영팀 계정 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(length = 1000)
    private String adminMemo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    public static ReceiptInquiry open(Participation participation, InquiryType type, Integer receivedUnits,
                                      DesiredResolution desiredResolution, String content, List<String> imageUrls) {
        ReceiptInquiry inquiry = new ReceiptInquiry();
        inquiry.participation = participation;
        inquiry.type = type;
        inquiry.receivedUnits = receivedUnits;
        inquiry.desiredResolution = desiredResolution;
        inquiry.content = content;
        if (imageUrls != null) {
            inquiry.imageUrls.addAll(imageUrls);
        }
        inquiry.status = InquiryStatus.OPEN;
        return inquiry;
    }
}
