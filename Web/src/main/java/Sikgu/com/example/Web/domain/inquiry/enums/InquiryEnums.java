package Sikgu.com.example.Web.domain.inquiry.enums;

/**
 * 수령 문의 관련 ENUM 모음 (docs/API.md 2장).
 */
public final class InquiryEnums {

    /** 문의 유형 */
    public enum InquiryType {
        /** 못 받음 */
        NOT_RECEIVED,
        /** 수량 부족 */
        SHORT_QUANTITY,
        /** 다른 상품·파손 */
        WRONG_OR_DAMAGED,
        /** 기타 */
        ETC
    }

    /** 참여자가 원하는 처리 (희망 사항) */
    public enum DesiredResolution {
        REDELIVER,
        PARTIAL_REFUND,
        FULL_REFUND
    }

    /** 처리 상태. 운영팀이 처리한다 (D10) */
    public enum InquiryStatus {
        /** 접수 */
        OPEN,
        /** 운영팀 처리중 */
        IN_PROGRESS,
        /** 처리 완료 */
        RESOLVED
    }

    /** 운영팀 최종 결정 (D10) */
    public enum InquiryResolution {
        /** 다시 받기 */
        REDELIVER,
        /** 부분 환불 */
        PARTIAL_REFUND,
        /** 전액 환불 */
        FULL_REFUND,
        /** 문제 없음 판정 */
        REJECTED
    }

    private InquiryEnums() {
    }
}
