package Sikgu.com.example.Web.domain.user.entity;

import Sikgu.com.example.Web.domain.user.enums.UserRole;
import Sikgu.com.example.Web.domain.user.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 사용자 (docs/DB.md 4.1 users)
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String schoolEmail;

    /** BCrypt 결과 문자열 (salt 포함 60자). salt 컬럼은 따로 두지 않는다 (D7) */
    @Column(nullable = false, length = 60)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(length = 50)
    private String universityName;

    /**
     * 집 위치 (GPS 위경도 + 동 이름). 가입 후 따로 설정하며, 설정 전에는 null.
     * 좌표는 본인에게만 보여주고, 다른 사용자에게는 동 이름만 노출한다 (D14).
     */
    @Column(length = 30)
    private String dongName;

    private Double latitude;

    private Double longitude;

    /** 평균 별점 캐시 */
    @Column(precision = 2, scale = 1)
    private BigDecimal ratingAvg;

    /** 거래 횟수 캐시 */
    @Column(nullable = false)
    private int tradeCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    private LocalDateTime suspendedUntil;

    /** 미수금 기한 초과 횟수. 10 이상이면 계정 정지 (D9) */
    @Column(nullable = false)
    private int overdueCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 회원가입. 집 위치는 비워 두고, 가입 후 {@link #changeLocation}으로 설정한다 */
    public static User create(String schoolEmail, String passwordHash, String nickname) {
        User user = new User();
        user.schoolEmail = schoolEmail;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.tradeCount = 0;
        user.role = UserRole.USER;
        user.status = UserStatus.ACTIVE;
        user.overdueCount = 0;
        return user;
    }

    /** 평균 별점 (후기가 없으면 null) */
    public Double ratingValue() {
        return ratingAvg == null ? null : ratingAvg.doubleValue();
    }

    /** 평균 별점 갱신 (후기 작성 시) */
    public void updateRating(BigDecimal ratingAvg) {
        this.ratingAvg = ratingAvg;
    }

    /** 정산완료된 공구 1건 추가 (호스트·참여자 모두) */
    public void increaseTradeCount() {
        this.tradeCount++;
    }

    /** 집 위치 설정·변경. 이미 올린 게시글에 저장된 위치는 바뀌지 않는다. */
    public void changeLocation(String dongName, Double latitude, Double longitude) {
        this.dongName = dongName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    /** 지금 정지 상태인지. suspendedUntil이 없으면 무기한 정지로 본다 (정지 기간 정책은 미정, D9) */
    public boolean isSuspended(LocalDateTime now) {
        return status == UserStatus.SUSPENDED
                && (suspendedUntil == null || suspendedUntil.isAfter(now));
    }
}
