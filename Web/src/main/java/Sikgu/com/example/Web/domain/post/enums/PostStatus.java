package Sikgu.com.example.Web.domain.post.enums;

/**
 * 공구 진행 상태 (docs/API.md 7.1).
 * RECRUITING → RECRUITED → ORDERED → SHIPPING → DISTRIBUTED → SETTLED, 예외 CANCELLED
 */
public enum PostStatus {
    /** 모집중 */
    RECRUITING,
    /** 모집완료 (총 단위가 다 참) */
    RECRUITED,
    /** 주문완료 (영수증 등록) */
    ORDERED,
    /** 배송중 */
    SHIPPING,
    /** 나눔 진행·수령확인 대기 (D6) */
    DISTRIBUTED,
    /** 정산완료 */
    SETTLED,
    /** 취소 */
    CANCELLED;

    /** 진행이 끝난 상태 (내 공구함 완료 탭) */
    public boolean isDone() {
        return this == SETTLED || this == CANCELLED;
    }
}
