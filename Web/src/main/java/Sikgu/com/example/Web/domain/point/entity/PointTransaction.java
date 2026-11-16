package Sikgu.com.example.Web.domain.point.entity;

import Sikgu.com.example.Web.domain.point.enums.PointTxType;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

// 포인트 거래 내역 (docs/DB.md 4.17 point_transactions)
// 포인트가 움직일 때마다 한 줄씩 추가만 함 (수정·삭제 없음)
@Entity
@Table(name = "point_transactions",
        indexes = @Index(name = "idx_point_tx_user_created", columnList = "user_id, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointTxType type;

    // 부호 포함: 나가면 음수, 들어오면 양수
    @Column(nullable = false)
    private long amount;

    // 이 거래 후 사용 가능 잔액
    @Column(nullable = false)
    private long balanceAfter;

    // 어느 공구 건인지 (충전이면 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    // 어느 보류 건인지 (결제·환급이면 있음, 충전·정산이면 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hold_id")
    private PointHold hold;

    @Column(nullable = false, length = 200)
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 공구 결제(보류): 잔액에서 빠짐
    public static PointTransaction payment(User user, PointHold hold, long balanceAfter, String description) {
        return create(user, PointTxType.PAYMENT, -hold.getAmount(), balanceAfter,
                hold.getPost(), hold, description);
    }

    // 환급: 보류됐던 만큼 돌아옴
    public static PointTransaction refund(User user, PointHold hold, long balanceAfter, String description) {
        return create(user, PointTxType.REFUND, hold.getAmount(), balanceAfter,
                hold.getPost(), hold, description);
    }

    // 정산: 호스트에게 공구 총액 지급
    public static PointTransaction settlement(User host, Post post, long amount, long balanceAfter,
                                              String description) {
        return create(host, PointTxType.SETTLEMENT, amount, balanceAfter, post, null, description);
    }

    private static PointTransaction create(User user, PointTxType type, long amount, long balanceAfter,
                                           Post post, PointHold hold, String description) {
        PointTransaction tx = new PointTransaction();
        tx.user = user;
        tx.type = type;
        tx.amount = amount;
        tx.balanceAfter = balanceAfter;
        tx.post = post;
        tx.hold = hold;
        tx.description = description;
        return tx;
    }
}