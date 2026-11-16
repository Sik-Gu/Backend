package Sikgu.com.example.Web.domain.participation.service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 수령 확인 정책 (D10).
 */
public final class ReceiptPolicy {

    /** 나눔 예정 시각 + 3일이 지나도 '받았어요'를 안 누르면 자동으로 수령 처리 */
    public static final Duration AUTO_CONFIRM_AFTER = Duration.ofDays(3);

    /** 자동 수령 처리 시각. 나눔 일정이 없으면 null */
    public static LocalDateTime autoConfirmAt(LocalDateTime distributeAt) {
        return distributeAt == null ? null : distributeAt.plus(AUTO_CONFIRM_AFTER);
    }

    private ReceiptPolicy() {
    }
}
