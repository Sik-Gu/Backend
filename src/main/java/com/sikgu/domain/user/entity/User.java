package com.sikgu.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String schoolEmail;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 12)
    private String nickname;

    private String dongName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(String schoolEmail, String password, String nickname) {
        this.schoolEmail = schoolEmail;
        this.password = password;
        this.nickname = nickname;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
