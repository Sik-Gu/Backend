package Sikgu.com.example.Web.domain.participation.scheduler;

import Sikgu.com.example.Web.domain.order.repository.PostOrderRepository;
import Sikgu.com.example.Web.domain.participation.service.ReceiptPolicy;
import Sikgu.com.example.Web.domain.participation.service.ReceiptService;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 자동 수령 처리 스케줄러 (D10).
 * 나눔 예정 시각 + 3일이 지난 공구를 찾아, '받았어요'를 안 누른 참여자를 받은 것으로 처리하고 정산한다.
 *
 * <p>공구마다 별도 트랜잭션이라, 한 공구가 실패해도 다른 공구는 처리된다.
 * 서버가 여러 대가 되면 중복 실행을 막는 장치(ShedLock 등)가 필요하다. 지금은 모놀리식 1대 기준.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoReceiptScheduler {

    private final PostOrderRepository orderRepository;
    private final ReceiptService receiptService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${receipt.auto-confirm.interval:PT10M}",
            initialDelayString = "${receipt.auto-confirm.initial-delay:PT1M}")
    public void run() {
        LocalDateTime before = LocalDateTime.now(clock).minus(ReceiptPolicy.AUTO_CONFIRM_AFTER);
        List<Long> postIds = orderRepository.findPostIdsToAutoConfirm(PostStatus.DISTRIBUTED, before);
        for (Long postId : postIds) {
            try {
                receiptService.autoConfirm(postId);
            } catch (Exception e) {
                log.error("자동 수령 처리 실패 post={}", postId, e);
            }
        }
    }
}
