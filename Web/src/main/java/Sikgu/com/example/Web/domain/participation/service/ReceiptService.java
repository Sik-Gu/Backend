package Sikgu.com.example.Web.domain.participation.service;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.ReceiptStatus;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수령 확인 ('받았어요')과 자동 수령 처리 (D10)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final PostRepository postRepository;
    private final ParticipationRepository participationRepository;
    private final SettlementService settlementService;
    private final Clock clock;

    /** '받았어요'. 마지막 사람이면 정산까지 한다 */
    @Transactional
    public ReceiptResult confirm(Long userId, Long postId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Post post = postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
        Participation participation = participationRepository.findByPostIdAndUserId(postId, userId)
                .filter(Participation::isActive)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_PARTICIPANT));
        post.requireStatus(PostStatus.DISTRIBUTED);
        if (participation.getReceiptStatus() == ReceiptStatus.RECEIVED) {
            throw new GeneralException(ErrorStatus.ALREADY_RECEIVED);
        }
        if (participation.getReceiptStatus() == ReceiptStatus.INQUIRING) {
            // 문의는 운영팀이 처리한다 (D10). 참여자가 스스로 끝낼 수 없다
            throw new GeneralException(ErrorStatus.INQUIRY_IN_PROGRESS);
        }

        participation.confirmReceipt(now, false);
        settlementService.settleIfAllReceived(post, now);
        return new ReceiptResult(participation.getReceiptStatus(), post.getStatus());
    }

    /**
     * 자동 수령 처리 (공구 하나). 나눔 예정 + 3일이 지나도 '받았어요'를 안 누른 사람을 받은 것으로 본다.
     * 문의중인 사람은 건드리지 않는다. 스케줄러가 공구마다 별도 트랜잭션으로 부른다.
     *
     * @return 자동 처리한 인원 수
     */
    @Transactional
    public int autoConfirm(Long postId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Post post = postRepository.findByIdForUpdate(postId).orElse(null);
        if (post == null || post.getStatus() != PostStatus.DISTRIBUTED) {
            return 0; // 그 사이 정산됐거나 상태가 바뀜
        }

        List<Participation> waiting = participationRepository.findActiveByPostId(postId).stream()
                .filter(pa -> pa.getReceiptStatus() == ReceiptStatus.WAITING)
                .toList();
        waiting.forEach(pa -> pa.confirmReceipt(now, true));
        boolean settled = settlementService.settleIfAllReceived(post, now);
        log.info("자동 수령 처리 post={} 처리 인원={} 정산={}", postId, waiting.size(), settled);
        return waiting.size();
    }

    public record ReceiptResult(ReceiptStatus receiptStatus, PostStatus postStatus) {
    }
}
