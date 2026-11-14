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

    @Column(nullable = false, unique = true, length = 20)
    private String loginId;

    @Column(nullable = false, unique = true, length = 100)
    private String schoolEmail;

    /** BCrypt 결과 문자열 (salt 포함 60자). salt 컬럼은 따로 두지 않는다 (D7) */
    @Column(nullable = false, length = 60)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(length = 50)
    private String universityName;

    /** 동 이름 + 위경도. 다른 사용자에게는 동 이름만 노출 (D14) */
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

    public static User create(String loginId, String schoolEmail, String passwordHash, String nickname,
                              String dongName, Double latitude, Double longitude) {
        User user = new User();
        user.loginId = loginId;
        user.schoolEmail = schoolEmail;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.dongName = dongName;
        user.latitude = latitude;
        user.longitude = longitude;
        user.tradeCount = 0;
        user.role = UserRole.USER;
        user.status = UserStatus.ACTIVE;
        user.overdueCount = 0;
        return user;
    }
}
