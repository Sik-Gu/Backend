package Sikgu.com.example.Web.domain.point.service;

import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.point.entity.PointHold;
import Sikgu.com.example.Web.domain.point.entity.PointTransaction;
import Sikgu.com.example.Web.domain.point.entity.PointWallet;
import Sikgu.com.example.Web.domain.point.enums.HoldStatus;
import Sikgu.com.example.Web.domain.point.repository.PointHoldRepository;
import Sikgu.com.example.Web.domain.point.repository.PointTransactionRepository;
import Sikgu.com.example.Web.domain.point.repository.PointWalletRepository;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.integration.point.PointClient;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// PointClient 진짜 구현. 공구 쪽(PostService, ParticipationService, SettlementService)이 호출함
// 호출한 쪽 트랜잭션 안에서 실행되고, 실패하면 GeneralException으로 전체 롤백
@Service
@RequiredArgsConstructor
@Transactional
public class PointService implements PointClient {

    private final PointWalletRepository pointWalletRepository;
    private final PointHoldRepository pointHoldRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ParticipationRepository participationRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    // 미수금 없음 (차액 정산을 안 하기로 해서 미수금이 생기지 않음)
    @Override
    public void validateNoUnpaidDebt(Long userId) {
    }

    // 결제: 사용 가능 → 보류, 보류 기록 + 거래 내역(PAYMENT)
    @Override
    public PaymentResult pay(Long userId, Long postId, Long participationId, long amount) {
        PointWallet wallet = getWallet(userId);
        wallet.hold(amount); // 잔액 부족이면 POINT409

        PointHold hold = pointHoldRepository.save(PointHold.create(
                participationRepository.getReferenceById(participationId),
                userRepository.getReferenceById(userId),
                postRepository.getReferenceById(postId),
                amount,
                LocalDateTime.now(clock)));
        pointTransactionRepository.save(
                PointTransaction.payment(hold.getUser(), hold, wallet.getAvailableBalance(), "공구 결제"));

        flushWallet();
        return new PaymentResult(amount, wallet.getAvailableBalance());
    }

    // 환급: 이 참여의 보관중 보류를 사용 가능 잔액으로 되돌림, 거래 내역(REFUND)
    @Override
    public RefundResult refund(Long userId, Long postId, Long participationId) {
        PointHold hold = pointHoldRepository.findByParticipationIdAndStatus(participationId, HoldStatus.HELD)
                .orElseThrow(() -> new IllegalStateException("환급할 보류가 없습니다: participationId=" + participationId));

        PointWallet wallet = getWallet(userId);
        wallet.release(hold.getAmount());
        hold.release(LocalDateTime.now(clock));
        pointTransactionRepository.save(
                PointTransaction.refund(hold.getUser(), hold, wallet.getAvailableBalance(), "공구 환급"));

        flushWallet();
        return new RefundResult(hold.getAmount(), wallet.getAvailableBalance());
    }

    // 정산: 이 공구의 보관중 보류를 전부 확정하고, 합계를 호스트에게 지급 (SETTLEMENT)
    @Override
    public void settle(Long postId, Long hostId) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<PointHold> holds = pointHoldRepository.findAllByPostIdAndStatus(postId, HoldStatus.HELD);

        long total = 0;
        for (PointHold hold : holds) {
            getWallet(hold.getUser().getId()).capture(hold.getAmount());
            hold.capture(now);
            total += hold.getAmount();
        }
        if (total == 0) {
            return;
        }

        PointWallet hostWallet = getWallet(hostId);
        hostWallet.deposit(total);
        pointTransactionRepository.save(PointTransaction.settlement(
                userRepository.getReferenceById(hostId),
                postRepository.getReferenceById(postId),
                total,
                hostWallet.getAvailableBalance(),
                "공구 정산"));

        flushWallet();
    }

    @Override
    @Transactional(readOnly = true)
    public PointSummary getSummary(Long userId) {
        PointWallet wallet = getWallet(userId);
        return new PointSummary(wallet.getAvailableBalance(), wallet.getHeldAmount(), 0L);
    }

    // 미수금 없음
    @Override
    @Transactional(readOnly = true)
    public boolean hasUnpaidDebt(Long userId, Long postId) {
        return false;
    }

    private PointWallet getWallet(Long userId) {
        // 회원가입 때 항상 만들어지므로, 없으면 데이터가 깨진 것 (버그)
        return pointWalletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("지갑이 없습니다: userId=" + userId));
    }

    // 지갑 변경을 지금 DB에 반영해서, 다른 요청과 동시에 바꿨으면 여기서 바로 POINT4091로 바꿈
    private void flushWallet() {
        try {
            pointWalletRepository.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new GeneralException(ErrorStatus.POINT_CONFLICT);
        }
    }
}