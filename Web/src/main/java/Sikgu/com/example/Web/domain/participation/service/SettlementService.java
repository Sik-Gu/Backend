package Sikgu.com.example.Web.domain.participation.service;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.ReceiptStatus;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.integration.chat.ChatClient;
import Sikgu.com.example.Web.integration.point.PointClient;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공구 정산. 참여자 전원이 받으면 호스트에게 정산한다.
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final ParticipationRepository participationRepository;
    private final PointClient pointClient;
    private final ChatClient chatClient;

    /**
     * 전원이 받았으면(RECEIVED) 정산완료 처리하고 true.
     * 문의중(INQUIRING)인 사람이 있으면 운영팀 처리를 기다리므로 정산하지 않는다 (D10).
     *
     * <p>호출하는 쪽이 게시글 행 락을 잡은 상태여야 한다 (마지막 두 명이 동시에 눌러도 한 번만 정산).
     */
    @Transactional
    public boolean settleIfAllReceived(Post post, LocalDateTime now) {
        List<Participation> participations = participationRepository.findActiveByPostId(post.getId());
        boolean allReceived = participations.stream()
                .allMatch(pa -> pa.getReceiptStatus() == ReceiptStatus.RECEIVED);
        if (!allReceived) {
            return false;
        }

        // 호스트에게 정산. 미수금이 있는 참여자 몫은 납부될 때 추가 지급 (D9), 인센티브 없음 (D11)
        pointClient.settle(post.getId(), post.getHost().getId());
        post.markSettled(now);
        participations.forEach(pa -> pa.getUser().increaseTradeCount());
        chatClient.sendSystemMessage(post.getId(), "모두 받았어요! 정산이 끝났어요. 3일 안에 후기를 남겨 주세요.");
        chatClient.close(post.getId());
        return true;
    }
}
