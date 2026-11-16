package Sikgu.com.example.Web.domain.user.enums;

/** 계정 상태. 미수금 기한 초과 누적 10회면 SUSPENDED (docs/DB.md D9) */
public enum UserStatus {
    ACTIVE,
    SUSPENDED
}
