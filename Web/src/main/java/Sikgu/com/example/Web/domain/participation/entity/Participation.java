package Sikgu.com.example.Web.domain.participation.entity;

import Sikgu.com.example.Web.domain.participation.enums.ParticipationRole;
import Sikgu.com.example.Web.domain.participation.enums.PaymentStatus;
import Sikgu.com.example.Web.domain.participation.enums.ReceiptStatus;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공구 참여 (docs/DB.md 4.4 participations). 호스트도 참여 한 건으로 저장한다 (D8).
 *
 * <p>(post_id, user_id) UNIQUE라서, 참여 취소 후 다시 참여하면 새로 만들지 않고 같은 행을 다시 쓴다.
 */
@Entity
@Table(name = "participations", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipationRole role;

    @Column(nullable = false)
    private int units;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReceiptStatus receiptStatus;

    /** '받았어요'를 누른 시각 또는 자동 수령 처리 시각 (D10) */
    private LocalDateTime receivedAt;

    /** 3일이 지나 자동으로 수령 처리됐는지 (D10) */
    @Column(nullable = false)
    private boolean autoConfirmed;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    public static Participation host(Post post, User user, int units, LocalDateTime now) {
        return create(post, user, ParticipationRole.HOST, units, now);
    }

    public static Participation participant(Post post, User user, int units, LocalDateTime now) {
        return create(post, user, ParticipationRole.PARTICIPANT, units, now);
    }

    private static Participation create(Post post, User user, ParticipationRole role, int units, LocalDateTime now) {
        Participation participation = new Participation();
        participation.post = post;
        participation.user = user;
        participation.role = role;
        participation.units = units;
        participation.paymentStatus = PaymentStatus.PAID;
        participation.receiptStatus = ReceiptStatus.WAITING;
        participation.autoConfirmed = false;
        participation.joinedAt = now;
        return participation;
    }

    // ---------- 조회용 ----------

    /** 참여 중인지 (결제 완료이고 환급되지 않음) */
    public boolean isActive() {
        return paymentStatus == PaymentStatus.PAID;
    }

    public boolean isHost() {
        return role == ParticipationRole.HOST;
    }

    /** 처음 결제한 금액 = 단위 × 단위당 가격 */
    public long paidPoint() {
        return (long) units * post.getUnitPrice();
    }

    // ---------- 변경 ----------

    /** 참여 취소 후 다시 참여 */
    public void rejoin(int units, LocalDateTime now) {
        this.units = units;
        this.paymentStatus = PaymentStatus.PAID;
        this.receiptStatus = ReceiptStatus.WAITING;
        this.receivedAt = null;
        this.autoConfirmed = false;
        this.joinedAt = now;
    }

    /** 환급 처리 (참여 취소, 공구 취소) */
    public void markRefunded() {
        this.paymentStatus = PaymentStatus.REFUNDED;
    }

    /**
     * 수령 확인.
     *
     * @param auto 3일이 지나 스케줄러가 처리한 경우 true (D10)
     */
    public void confirmReceipt(LocalDateTime now, boolean auto) {
        this.receiptStatus = ReceiptStatus.RECEIVED;
        this.receivedAt = now;
        this.autoConfirmed = auto;
    }

    /** 수령 문제 문의 시작 */
    public void startInquiry() {
        this.receiptStatus = ReceiptStatus.INQUIRING;
    }
}
