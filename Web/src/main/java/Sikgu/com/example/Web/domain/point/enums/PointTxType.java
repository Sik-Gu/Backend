package Sikgu.com.example.Web.domain.point.enums;

public enum PointTxType {
    CHARGE,      // 충전 (+)
    PAYMENT,     // 공구 결제 = 보류 (-)
    REFUND,      // 환급 (+) 참여 취소·공구 취소
    SETTLEMENT   // 정산 (+) 호스트에게 지급
}