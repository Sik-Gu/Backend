package Sikgu.com.example.Web.domain.inquiry.service;

import Sikgu.com.example.Web.domain.inquiry.dto.request.InquiryCreateRequest;
import Sikgu.com.example.Web.domain.inquiry.dto.response.InquiryResponses.InquiryCreateResponse;
import Sikgu.com.example.Web.domain.inquiry.dto.response.InquiryResponses.InquiryItem;
import Sikgu.com.example.Web.domain.inquiry.entity.ReceiptInquiry;
import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.InquiryType;
import Sikgu.com.example.Web.domain.inquiry.repository.ReceiptInquiryRepository;
import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.ReceiptStatus;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.integration.chat.ChatClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수령 문제 문의 (docs/API.md 7장). 처리는 운영팀이 한다 (D10, 관리자 화면은 나중에).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final PostRepository postRepository;
    private final ParticipationRepository participationRepository;
    private final ReceiptInquiryRepository inquiryRepository;
    private final ChatClient chatClient;

    /**
     * 문의 등록. 내 수령 상태가 문의중이 되어 자동 수령·정산 대상에서 빠진다.
     * 받았어요·자동 수령과 동시에 일어나지 않게 게시글 락을 잡는다.
     */
    @Transactional
    public InquiryCreateResponse create(Long userId, Long postId, InquiryCreateRequest request) {
        Post post = postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
        Participation participation = participationRepository.findByPostIdAndUserId(postId, userId)
                .filter(Participation::isActive)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_PARTICIPANT));
        post.requireStatus(PostStatus.DISTRIBUTED);
        if (participation.getReceiptStatus() == ReceiptStatus.RECEIVED) {
            throw new GeneralException(ErrorStatus.INQUIRY_AFTER_RECEIVED);
        }
        if (participation.getReceiptStatus() == ReceiptStatus.INQUIRING) {
            throw new GeneralException(ErrorStatus.INQUIRY_IN_PROGRESS);
        }
        validate(request, participation);

        ReceiptInquiry inquiry = inquiryRepository.save(ReceiptInquiry.open(participation, request.type(),
                request.receivedUnits(), request.desiredResolution(), request.content(), request.imageUrls()));
        participation.startInquiry();
        chatClient.sendSystemMessage(postId, "수령 문의가 접수됐어요. 운영팀이 확인할 때까지 정산이 미뤄져요.");

        return new InquiryCreateResponse(inquiry.getId(), participation.getReceiptStatus(), inquiry.getStatus());
    }

    /** 문의 목록. 호스트는 전체, 참여자는 자기 문의만 */
    public List<InquiryItem> list(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
        List<ReceiptInquiry> inquiries;
        if (post.isHost(userId)) {
            inquiries = inquiryRepository.findByPostId(postId);
        } else {
            participationRepository.findByPostIdAndUserId(postId, userId)
                    .filter(Participation::isActive)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_PARTICIPANT));
            inquiries = inquiryRepository.findByPostIdAndUserId(postId, userId);
        }
        return inquiries.stream().map(InquiryService::toItem).toList();
    }

    private static void validate(InquiryCreateRequest request, Participation participation) {
        if (request.type() == InquiryType.SHORT_QUANTITY) {
            Integer received = request.receivedUnits();
            if (received == null || received < 0 || received >= participation.getUnits()) {
                throw new GeneralException(ErrorStatus.INVALID_RECEIVED_UNITS);
            }
        }
        if (request.type() == InquiryType.WRONG_OR_DAMAGED
                && (request.imageUrls() == null || request.imageUrls().isEmpty())) {
            throw new GeneralException(ErrorStatus.INQUIRY_IMAGE_REQUIRED);
        }
    }

    private static InquiryItem toItem(ReceiptInquiry inquiry) {
        return new InquiryItem(inquiry.getId(), inquiry.getParticipation().getUser().getId(),
                inquiry.getParticipation().getUser().getNickname(), inquiry.getType(), inquiry.getReceivedUnits(),
                inquiry.getDesiredResolution(), inquiry.getContent(), List.copyOf(inquiry.getImageUrls()),
                inquiry.getStatus(), inquiry.getResolution(), inquiry.getRefundAmount(), inquiry.getCreatedAt(),
                inquiry.getResolvedAt());
    }
}
