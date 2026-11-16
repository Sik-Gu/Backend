package Sikgu.com.example.Web.domain.participation.service;

import Sikgu.com.example.Web.domain.block.service.BlockService;
import Sikgu.com.example.Web.domain.participation.dto.response.ParticipationResponses.JoinResponse;
import Sikgu.com.example.Web.domain.participation.dto.response.ParticipationResponses.LeaveResponse;
import Sikgu.com.example.Web.domain.participation.dto.response.ParticipationResponses.ParticipantStatus;
import Sikgu.com.example.Web.domain.participation.dto.response.ParticipationResponses.ParticipationStatusResponse;
import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.LeaveReason;
import Sikgu.com.example.Web.domain.participation.enums.ReceiptStatus;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.integration.chat.ChatClient;
import Sikgu.com.example.Web.integration.point.PointClient;
import Sikgu.com.example.Web.integration.point.PointClient.PaymentResult;
import Sikgu.com.example.Web.integration.point.PointClient.RefundResult;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공구 참여·참여 취소·결제·수령 현황 (docs/API.md 7장).
 *
 * <p>참여·취소는 게시글 행에 비관적 락을 먼저 잡는다. 마지막 단위를 여러 명이 동시에 눌러도
 * 한 명씩 처리되어 총 단위를 넘지 않는다 (D3). 락 순서는 항상 게시글 → 지갑 (D4).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationService {

    private final PostRepository postRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final BlockService blockService;
    private final PointClient pointClient;
    private final ChatClient chatClient;
    private final Clock clock;

    /** 참여 + 즉시 결제(보류). 단위가 다 차면 모집완료 */
    @Transactional
    public JoinResponse join(Long userId, Long postId, int units) {
        LocalDateTime now = LocalDateTime.now(clock);
        pointClient.validateNoUnpaidDebt(userId); // D9

        Post post = getPostForUpdate(postId);
        if (post.isHost(userId)) {
            throw new GeneralException(ErrorStatus.ALREADY_PARTICIPATED);
        }
        if (post.getStatus() != PostStatus.RECRUITING || post.isDeadlinePassed(now)) {
            throw new GeneralException(ErrorStatus.INVALID_POST_STATUS);
        }
        if (blockService.isBlockedBetween(userId, post.getHost().getId())) {
            throw new GeneralException(ErrorStatus.BLOCKED_HOST);
        }

        Participation participation = participationRepository.findByPostIdAndUserId(postId, userId).orElse(null);
        if (participation != null && participation.isActive()) {
            throw new GeneralException(ErrorStatus.ALREADY_PARTICIPATED);
        }

        boolean full = post.addParticipant(units); // 남은 단위 초과면 PART4001
        if (participation == null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
            participation = participationRepository.save(Participation.participant(post, user, units, now));
        } else {
            // (post_id, user_id) UNIQUE라서 취소했던 참여를 다시 쓴다
            participation.rejoin(units, now);
        }

        PaymentResult payment = pointClient.pay(userId, postId, participation.getId(), participation.paidPoint());
        chatClient.join(postId, userId);
        if (full) {
            post.markRecruited(now);
            chatClient.sendSystemMessage(postId, "모집이 완료됐어요! 호스트가 주문을 진행해요.");
        }

        return new JoinResponse(participation.getId(), payment.paidPoint(), payment.balanceAfter(),
                post.getStatus(), chatClient.findRoomId(postId).orElse(null));
    }

    /**
     * 참여 취소(나가기) + 전액 환급. 주문 전(모집중·모집완료)에서만. 모집완료였으면 모집중으로 돌아가 빈자리를 다시 모집한다.
     *
     * @param reason PLACE_DISAGREEMENT면 나눔 장소 의견이 맞지 않아 나가는 경우. 호스트가 장소를 제안한 뒤에만 쓸 수 있다
     */
    @Transactional
    public LeaveResponse leave(Long userId, Long postId, LeaveReason reason) {
        LocalDateTime now = LocalDateTime.now(clock);
        Post post = getPostForUpdate(postId);
        Participation participation = participationRepository.findByPostIdAndUserId(postId, userId)
                .filter(Participation::isActive)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_PARTICIPANT));
        if (participation.isHost()) {
            throw new GeneralException(ErrorStatus.HOST_CANNOT_LEAVE);
        }
        post.requireBeforeOrder();
        if (reason == LeaveReason.PLACE_DISAGREEMENT && !post.hasDistributionPlace()) {
            throw new GeneralException(ErrorStatus.DISTRIBUTION_PLACE_NOT_PROPOSED);
        }

        post.removeParticipant(participation.getUnits());
        RefundResult refund = pointClient.refund(userId, postId, participation.getId());
        participation.leave(reason, now);
        chatClient.leave(postId, userId);
        if (reason == LeaveReason.PLACE_DISAGREEMENT) {
            chatClient.sendSystemMessage(postId, participation.getUser().getNickname()
                    + "님이 나눔 장소 의견이 맞지 않아 공구에서 나갔어요. 빈자리를 다시 모집해요.");
        }

        return new LeaveResponse(participation.paidPoint(), refund.balanceAfter(), post.getStatus(), reason);
    }

    /** 결제·수령 현황 (호스트·참여자만) */
    public ParticipationStatusResponse getStatus(Long userId, Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new GeneralException(ErrorStatus.POST_NOT_FOUND);
        }
        List<Participation> participations = participationRepository.findActiveByPostId(postId);
        boolean member = participations.stream().anyMatch(pa -> pa.getUser().getId().equals(userId));
        if (!member) {
            throw new GeneralException(ErrorStatus.NOT_PARTICIPANT);
        }

        List<ParticipantStatus> statuses = participations.stream()
                .map(pa -> new ParticipantStatus(pa.getUser().getId(), pa.getUser().getNickname(), pa.getRole(),
                        pa.getUnits(), pa.paidPoint(), pa.getPaymentStatus(), pa.getReceiptStatus(),
                        pa.getUser().getId().equals(userId)))
                .toList();
        int receivedCount = (int) participations.stream()
                .filter(pa -> pa.getReceiptStatus() == ReceiptStatus.RECEIVED).count();
        return new ParticipationStatusResponse(participations.size(), receivedCount, participations.size(),
                statuses);
    }

    private Post getPostForUpdate(Long postId) {
        return postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
    }
}
