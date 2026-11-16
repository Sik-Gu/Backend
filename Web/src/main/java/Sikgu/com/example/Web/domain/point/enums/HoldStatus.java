package Sikgu.com.example.Web.domain.point.enums;

public enum HoldStatus {
    HELD,      // 보관중 (참여해서 묶여 있음)
    CAPTURED,  // 정산됨 (호스트에게 지급)
    RELEASED   // 환급됨 (참여 취소·공구 취소)
}