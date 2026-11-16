package Sikgu.com.example.Web.domain.point.entity;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.point.enums.HoldStatus;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 포인트 보류 (docs/DB.md 4.16 point_holds)
// 참여할 때 HELD로 생성 → 취소면 RELEASED, 정산이면 CAPTURED
@Entity
@Table(name = "point_holds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hold_id")
    private Long id;

    // 재참여하면 같은 참여에 보류가 또 생기므로 UNIQUE 아님
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    private Participation participation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HoldStatus status;

    @Column(nullable = false)
    private LocalDateTime heldAt;

    // 정산되거나 환급된 시각 (보관중이면 null)
    private LocalDateTime resolvedAt;

    public static PointHold create(Participation participation, User user, Post post,
                                   long amount, LocalDateTime now) {
        PointHold hold = new PointHold();
        hold.participation = participation;
        hold.user = user;
        hold.post = post;
        hold.amount = amount;
        hold.status = HoldStatus.HELD;
        hold.heldAt = now;
        return hold;
    }

    // 환급 (참여 취소·공구 취소)
    public void release(LocalDateTime now) {
        this.status = HoldStatus.RELEASED;
        this.resolvedAt = now;
    }

    // 정산 확정 (호스트에게 지급)
    public void capture(LocalDateTime now) {
        this.status = HoldStatus.CAPTURED;
        this.resolvedAt = now;
    }
}