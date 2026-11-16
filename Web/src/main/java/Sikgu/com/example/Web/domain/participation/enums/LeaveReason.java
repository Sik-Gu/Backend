package Sikgu.com.example.Web.domain.participation.enums;

/** 참여자가 공구에서 나간 이유. 나가기는 주문 전(모집중·모집완료)에만 되고, 전액 환급된다 */
public enum LeaveReason {
    /** 단순 변심 등 일반 취소 */
    CHANGED_MIND,
    /** 나눔 장소를 정하다가 의견이 맞지 않음 */
    PLACE_DISAGREEMENT
}
