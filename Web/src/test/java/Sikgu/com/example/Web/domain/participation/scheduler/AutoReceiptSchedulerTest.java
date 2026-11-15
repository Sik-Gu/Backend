package Sikgu.com.example.Web.domain.participation.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.order.repository.PostOrderRepository;
import Sikgu.com.example.Web.domain.participation.service.ReceiptService;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutoReceiptSchedulerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    PostOrderRepository orderRepository;
    @Mock
    ReceiptService receiptService;

    @Test
    void 나눔_예정_3일이_지난_공구를_처리하고_하나가_실패해도_나머지는_처리한다() {
        LocalDateTime threeDaysAgo = LocalDateTime.now(CLOCK).minusDays(3);
        given(orderRepository.findPostIdsToAutoConfirm(PostStatus.DISTRIBUTED, threeDaysAgo))
                .willReturn(List.of(1L, 2L));
        given(receiptService.autoConfirm(1L)).willThrow(new RuntimeException("boom"));

        new AutoReceiptScheduler(orderRepository, receiptService, CLOCK).run();

        verify(receiptService).autoConfirm(1L);
        verify(receiptService).autoConfirm(2L);
    }
}
