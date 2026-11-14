package Sikgu.com.example.Web.domain.point.entity;

import Sikgu.com.example.Web.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 포인트 지갑 (docs/DB.md 4.14 point_wallets). 회원가입 시 잔액 0으로 함께 만든다.
 */
@Entity
@Table(name = "point_wallets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** 사용 가능 잔액 */
    @Column(nullable = false)
    private long availableBalance;

    /** 진행 중인 공구에 보류된 금액 */
    @Column(nullable = false)
    private long heldAmount;

    /** 동시성 제어용 낙관적 락 (D4) */
    @Version
    private Long version;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static PointWallet createFor(User user) {
        PointWallet wallet = new PointWallet();
        wallet.user = user;
        wallet.availableBalance = 0L;
        wallet.heldAmount = 0L;
        return wallet;
    }
}
