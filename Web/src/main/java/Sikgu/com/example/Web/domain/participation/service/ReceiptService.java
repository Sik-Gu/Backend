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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수령 확인 ('받았어요'). 자동 수령 처리는 없다 — 참여자가 직접 누르기 전까지 수령 대기로 남는다 (D17)
 */
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

        participation.confirmReceipt(now);
        settlementService.settleIfAllReceived(post, now);
        return new ReceiptResult(participation.getReceiptStatus(), post.getStatus());
    }

    public record ReceiptResult(ReceiptStatus receiptStatus, PostStatus postStatus) {
    }
}
