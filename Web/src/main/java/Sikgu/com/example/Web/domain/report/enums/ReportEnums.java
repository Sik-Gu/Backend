package Sikgu.com.example.Web.domain.report.enums;

/**
 * 신고 관련 ENUM 모음 (docs/API.md 2장).
 */
public final class ReportEnums {

    /** 신고 사유. 신고 대상은 항상 사람 (게시글 신고 없음) */
    public enum ReportReason {
        /** 채팅 욕설·비매너 */
        CHAT_ABUSE,
        /** 거래 규칙 위반 */
        TRADE_VIOLATION,
        /** 기타 */
        ETC
    }

    /** 처리 상태. 처리는 운영팀이 한다 (관리자 화면은 나중에) */
    public enum ReportStatus {
        PENDING,
        DONE
    }

    /** 거래 규칙 위반이 일어난 단계 (TRADE_VIOLATION일 때) */
    public enum ViolationStage {
        RECRUITING,
        PAYMENT,
        ORDER,
        RECEIPT,
        SHIPPING,
        DISTRIBUTION
    }

    private ReportEnums() {
    }
}
