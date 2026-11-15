package Sikgu.com.example.Web.integration.point;

/**
 * 공구 기능이 포인트 기능을 호출하는 창구.
 *
 * <p>포인트 담당 팀원이 이 인터페이스를 구현한다. 구현이 들어오기 전까지는 {@link FakePointClient}가 대신 동작한다.
 * 모든 메서드는 호출한 쪽의 트랜잭션 안에서 실행된다 (전파 REQUIRED). 실패하면 GeneralException을 던져 전체를 롤백한다.
 *
 * <p>락 순서: 호출하는 쪽이 게시글 행에 비관적 락을 먼저 잡은 뒤 이 메서드를 부른다. 지갑 락은 그 다음이다 (docs/DB.md D4).
 */
public interface PointClient {

    /**
     * 미납 미수금이 있으면 공구 참여·등록을 막는다 (D9).
     *
     * @throws Sikgu.com.example.Web.global.apiPayload.exception.GeneralException POINT4031
     */
    void validateNoUnpaidDebt(Long userId);

    /**
     * 공구 결제: 사용 가능 잔액에서 빼서 보류(HELD)한다. 공구 등록(호스트 몫)과 참여에서 부른다 (D8).
     *
     * @throws Sikgu.com.example.Web.global.apiPayload.exception.GeneralException POINT409 잔액 부족, POINT4091 동시 변경 충돌
     */
    PaymentResult pay(Long userId, Long postId, Long participationId, long amount);

    /**
     * 참여 한 건의 보류 포인트를 전부 환급(RELEASED)한다. 참여 취소, 공구 취소에서 부른다.
     */
    RefundResult refund(Long userId, Long postId, Long participationId);

    /**
     * 영수증 차액 정산 (D9).
     *
     * @param amount 양수면 추가 청구(ADJUSTMENT_CHARGE, 잔액이 모자라면 미수금 생성), 음수면 환급(ADJUSTMENT_REFUND)
     */
    AdjustmentResult adjust(Long userId, Long postId, Long participationId, long amount);

    /**
     * 공구 정산: 이 공구의 보류를 모두 확정(CAPTURED)하고 호스트에게 지급한다 (SETTLEMENT).
     * 미수금이 있는 참여자 몫은 납부될 때 호스트에게 추가 지급한다 (D9). 인센티브는 없다 (D11).
     */
    void settle(Long postId, Long hostId);

    /** 내 정보 화면용 포인트 요약 */
    PointSummary getSummary(Long userId);

    /** 이 공구에서 생긴 미납 미수금이 있는지 (내 공구함 PAY_DEBT 표시용) */
    boolean hasUnpaidDebt(Long userId, Long postId);

    record PaymentResult(long paidPoint, long balanceAfter) {
    }

    record RefundResult(long refundedPoint, long balanceAfter) {
    }

    /**
     * @param amount        청구(+) 또는 환급(-)한 금액
     * @param chargedAmount 실제로 지갑에서 빠진 금액 (청구일 때)
     * @param debtAmount    잔액 부족으로 미수금이 된 금액 (청구일 때)
     */
    record AdjustmentResult(long amount, long chargedAmount, long debtAmount) {
    }

    record PointSummary(long availableBalance, long heldAmount, long unpaidDebt) {
    }
}
