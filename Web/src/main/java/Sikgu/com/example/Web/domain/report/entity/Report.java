package Sikgu.com.example.Web.domain.report.entity;

import Sikgu.com.example.Web.domain.report.enums.ReportEnums.ReportReason;
import Sikgu.com.example.Web.domain.report.enums.ReportEnums.ReportStatus;
import Sikgu.com.example.Web.domain.report.enums.ReportEnums.ViolationStage;
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
 * 사용자 신고 (docs/DB.md 4.9 reports). 신고 대상은 사람만 (게시글 신고·게시글 연결 없음).
 * 처리 전(PENDING)으로 저장만 하고 운영팀이 처리한다.
 */
@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ViolationStage violationStage;

    @Column(length = 1000)
    private String content;

    /** 관련 채팅 메시지 ID (채팅 기능의 messages 테이블) */
    @ElementCollection
    @CollectionTable(name = "report_messages", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "message_id", nullable = false)
    private List<Long> messageIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "report_images", joinColumns = @JoinColumn(name = "report_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "image_url", nullable = false, length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    public static Report create(User reporter, User target, ReportReason reason,
                                ViolationStage violationStage, String content, List<Long> messageIds,
                                List<String> imageUrls) {
        Report report = new Report();
        report.reporter = reporter;
        report.target = target;
        report.reason = reason;
        report.violationStage = violationStage;
        report.content = content;
        if (messageIds != null) {
            report.messageIds.addAll(messageIds);
        }
        if (imageUrls != null) {
            report.imageUrls.addAll(imageUrls);
        }
        report.status = ReportStatus.PENDING;
        return report;
    }
}
