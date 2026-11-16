package Sikgu.com.example.Web.domain.participation.service;

import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.dto.request.DistributionPlaceRequest;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.DistributionPlaceInfo;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.post.service.PostService;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.integration.chat.ChatClient;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 나눔 장소 합의 (주문 전).
 * <ol>
 *   <li>호스트가 나눔 장소를 제안한다 (모집중·모집완료). 바꾸면 참여자 동의는 모두 초기화된다.</li>
 *   <li>참여자는 동의하거나, 의견이 맞지 않으면 나간다 (참여 취소 API, reason=PLACE_DISAGREEMENT, 전액 환급).</li>
 *   <li>모집완료 + 전원 동의여야 호스트가 영수증을 등록(주문)할 수 있다.</li>
 * </ol>
 * 참여·나가기와 같은 게시글 행 락을 잡아서, 동의 집계 중에 인원이 바뀌지 않게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DistributionPlaceService {

    private final PostRepository postRepository;
    private final ParticipationRepository participationRepository;
    private final ChatClient chatClient;
    private final Clock clock;

    /** 나눔 장소 제안·변경 (호스트) */
    @Transactional
    public DistributionPlaceInfo propose(Long userId, Long postId, DistributionPlaceRequest request) {
        Post post = getPostForUpdate(postId);
        if (!post.isHost(userId)) {
            throw new GeneralException(ErrorStatus.NOT_POST_HOST);
        }
        post.proposeDistributionPlace(request.name().trim(), request.latitude(), request.longitude(),
                LocalDateTime.now(clock)); // 주문 이후면 POST409

        List<Participation> participations = participationRepository.findActiveByPostId(postId);
        participations.forEach(Participation::resetPlaceAgreement);
        chatClient.sendSystemMessage(postId, "호스트가 나눔 장소로 '" + post.getDistributionPlaceName()
                + "'을(를) 제안했어요. 괜찮으면 동의를, 의견이 맞지 않으면 나가기를 눌러 주세요.");

        return PostService.distributionPlace(post, participations, findMine(participations, userId));
    }

    /** 제안된 나눔 장소에 동의 (참여자) */
    @Transactional
    public DistributionPlaceInfo agree(Long userId, Long postId) {
        Post post = getPostForUpdate(postId);
        post.requireBeforeOrder();
        if (!post.hasDistributionPlace()) {
            throw new GeneralException(ErrorStatus.DISTRIBUTION_PLACE_NOT_PROPOSED);
        }
        List<Participation> participations = participationRepository.findActiveByPostId(postId);
        Participation mine = findMine(participations, userId);
        if (mine == null) {
            throw new GeneralException(ErrorStatus.NOT_PARTICIPANT);
        }
        if (mine.isPlaceAgreed()) {
            throw new GeneralException(ErrorStatus.PLACE_ALREADY_AGREED);
        }
        mine.agreePlace();

        DistributionPlaceInfo info = PostService.distributionPlace(post, participations, mine);
        if (info.allAgreed() && post.getStatus() == PostStatus.RECRUITED) {
            chatClient.sendSystemMessage(postId, "모두 나눔 장소에 동의했어요! 호스트가 주문을 진행해요.");
        }
        return info;
    }

    private static Participation findMine(List<Participation> participations, Long userId) {
        return participations.stream()
                .filter(pa -> pa.getUser().getId().equals(userId))
                .findFirst().orElse(null);
    }

    private Post getPostForUpdate(Long postId) {
        return postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
    }
}
