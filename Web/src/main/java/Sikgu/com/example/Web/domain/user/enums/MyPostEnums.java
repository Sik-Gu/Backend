package Sikgu.com.example.Web.domain.user.enums;

/**
 * 내 공구함 관련 ENUM 모음 (docs/API.md 2장).
 */
public final class MyPostEnums {

    /** 탭 */
    public enum MyPostTab {
        /** 진행중 */
        ONGOING,
        /** 완료 (정산완료·취소) */
        DONE
    }

    /** 필터 */
    public enum MyPostFilter {
        ALL,
        /** 내가 연 공구 */
        HOST,
        /** 참여한 공구 */
        JOINED,
        /** 내가 할 일이 있는 공구 */
        ACTION_REQUIRED
    }

    /** 내가 해야 할 일 */
    public enum ActionRequired {
        /** 호스트: 나눔 장소를 아직 제안하지 않음 (모집완료) */
        PROPOSE_PLACE,
        /** 참여자: 호스트가 제안한 나눔 장소에 동의 또는 나가기 */
        AGREE_PLACE,
        /** 호스트: 모집완료 + 전원 장소 동의 → 영수증 등록 */
        ORDER,
        /** 호스트: 주문완료 → 배송 시작 */
        SHIP,
        /** 호스트: 배송중 → 나눔 일정 입력 */
        DISTRIBUTE,
        /** 참여자: 나눔 진행 중 → 받았어요 */
        CONFIRM_RECEIPT,
        /** 정산 후 3일 안에 안 쓴 후기가 있음 (D12) */
        WRITE_REVIEW,
        /** 이 공구에서 생긴 미수금이 있음 (D9) */
        PAY_DEBT
    }

    private MyPostEnums() {
    }
}
