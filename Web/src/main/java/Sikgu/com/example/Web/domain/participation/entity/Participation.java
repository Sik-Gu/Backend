package Sikgu.com.example.Web.domain.participation.entity;

import Sikgu.com.example.Web.domain.participation.enums.LeaveReason;
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

    /** '받았어요'를 누른 시각 */
    private LocalDateTime receivedAt;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    /** 호스트가 제안한 나눔 장소에 동의했는지. 장소가 바뀌면 다시 false */
    @Column(nullable = false)
    private boolean placeAgreed;

    /** 참여를 그만둔 이유 (참여 중이면 null) */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private LeaveReason leaveReason;

    private LocalDateTime leftAt;

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
        participation.placeAgreed = role == ParticipationRole.HOST; // 호스트는 자기가 제안하므로 항상 동의
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
        this.placeAgreed = false;
        this.leaveReason = null;
        this.leftAt = null;
        this.joinedAt = now;
    }

    /** 환급 처리 (공구 취소) */
    public void markRefunded() {
        this.paymentStatus = PaymentStatus.REFUNDED;
    }

    /** 참여자가 스스로 나감 (환급 포함). 나눔 장소 의견이 맞지 않아 나가는 경우도 여기로 */
    public void leave(LeaveReason reason, LocalDateTime now) {
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.placeAgreed = false;
        this.leaveReason = reason;
        this.leftAt = now;
    }

    /** 제안된 나눔 장소에 동의 */
    public void agreePlace() {
        this.placeAgreed = true;
    }

    /** 나눔 장소가 바뀌어 동의를 다시 받아야 함. 호스트는 제안한 사람이라 동의 유지 */
    public void resetPlaceAgreement() {
        this.placeAgreed = isHost();
    }

    /** 수령 확인 ('받았어요'). 자동 처리는 없다 (D22) */
    public void confirmReceipt(LocalDateTime now) {
        this.receiptStatus = ReceiptStatus.RECEIVED;
        this.receivedAt = now;
    }

    /** 수령 문제 문의 시작 */
    public void startInquiry() {
        this.receiptStatus = ReceiptStatus.INQUIRING;
    }
}
