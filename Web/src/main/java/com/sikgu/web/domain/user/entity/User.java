package com.sikgu.web.domain.user.entity;

import com.sikgu.web.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String loginId;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    // 이메일 인증 붙이기 전까지 NULL 허용
    @Column(unique = true, length = 100)
    private String schoolEmail;

    @Column(length = 50)
    private String universityName;

    @Column(nullable = false, length = 50)
    private String dongName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double rating;

    @Builder
    private User(String loginId, String password, String nickname,
                 String dongName, Double latitude, Double longitude) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
        this.dongName = dongName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rating = 0.0;
    }
}