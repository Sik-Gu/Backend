package Sikgu.com.example.Web.integration.point;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 포인트 기능이 구현되기 전까지 쓰는 가짜 구현. 모든 결제가 성공하고 잔액은 0으로 응답한다.
 *
 * <p>포인트 담당 팀원이 {@link PointClient}를 구현하면 이 클래스를 지우면 된다.
 */
@Slf4j
@Component
public class FakePointClient implements PointClient {

    @Override
    public void validateNoUnpaidDebt(Long userId) {
        // 미수금 없음으로 본다
    }

    @Override
    public PaymentResult pay(Long userId, Long postId, Long participationId, long amount) {
        log.debug("[FakePoint] pay user={} post={} participation={} amount={}", userId, postId, participationId, amount);
        return new PaymentResult(amount, 0L);
    }

    @Override
    public RefundResult refund(Long userId, Long postId, Long participationId) {
        log.debug("[FakePoint] refund user={} post={} participation={}", userId, postId, participationId);
        return new RefundResult(0L, 0L);
    }

    @Override
    public void settle(Long postId, Long hostId) {
        log.debug("[FakePoint] settle post={} host={}", postId, hostId);
    }

    @Override
    public PointSummary getSummary(Long userId) {
        return new PointSummary(0L, 0L, 0L);
    }

    @Override
    public boolean hasUnpaidDebt(Long userId, Long postId) {
        return false;
    }
}
