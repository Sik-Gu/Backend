package Sikgu.com.example.Web.domain.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.inquiry.dto.request.InquiryCreateRequest;
import Sikgu.com.example.Web.domain.inquiry.dto.response.InquiryResponses.InquiryCreateResponse;
import Sikgu.com.example.Web.domain.inquiry.entity.ReceiptInquiry;
import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.DesiredResolution;
import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.InquiryStatus;
import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.InquiryType;
import Sikgu.com.example.Web.domain.inquiry.repository.ReceiptInquiryRepository;
import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.participation.enums.ReceiptStatus;
import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.integration.chat.ChatClient;
import Sikgu.com.example.Web.support.Fixtures;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InquiryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);

    @Mock
    PostRepository postRepository;
    @Mock
    ParticipationRepository participationRepository;
    @Mock
    ReceiptInquiryRepository inquiryRepository;
    @Mock
    ChatClient chatClient;

    @InjectMocks
    InquiryService service;

    User host = Fixtures.user(10L);
    User member = Fixtures.user(20L);
    Post post;
    Participation mine;

    @BeforeEach
    void setUp() {
        post = Fixtures.post(3L, host, NOW.plusDays(2));
        post.addParticipant(2);
        post.addParticipant(4);
        post.markRecruited(NOW);
        post.markOrdered(NOW);
        post.markShipping(NOW);
        post.markDistributed(NOW);
        mine = Participation.participant(post, member, 4, NOW);
        given(postRepository.findByIdForUpdate(3L)).willReturn(Optional.of(post));
        given(postRepository.findById(3L)).willReturn(Optional.of(post));
        given(participationRepository.findByPostIdAndUserId(3L, 20L)).willReturn(Optional.of(mine));
        given(inquiryRepository.save(any(ReceiptInquiry.class))).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void 문의하면_문의중이_되어_정산에서_빠진다() {
        InquiryCreateResponse response = service.create(20L, 3L, request(InquiryType.NOT_RECEIVED, null, List.of()));

        assertThat(response.receiptStatus()).isEqualTo(ReceiptStatus.INQUIRING);
        assertThat(response.inquiryStatus()).isEqualTo(InquiryStatus.OPEN);
        assertThat(mine.getReceiptStatus()).isEqualTo(ReceiptStatus.INQUIRING);
        verify(chatClient).sendSystemMessage(any(), any());
    }

    @Test
    void 수량_부족은_받은_단위가_0이상_내_단위_미만이어야_한다() {
        assertThat(service.create(20L, 3L, request(InquiryType.SHORT_QUANTITY, 3, List.of())).inquiryStatus())
                .isEqualTo(InquiryStatus.OPEN);
    }

    @Test
    void 수량_부족인데_받은_단위가_없거나_범위를_벗어나면_INQ4001() {
        assertThatThrownBy(() -> service.create(20L, 3L, request(InquiryType.SHORT_QUANTITY, null, List.of())))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_RECEIVED_UNITS);
        assertThatThrownBy(() -> service.create(20L, 3L, request(InquiryType.SHORT_QUANTITY, 4, List.of())))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_RECEIVED_UNITS);
        verify(inquiryRepository, never()).save(any());
    }

    @Test
    void 다른_상품_파손인데_사진이_없으면_INQ4002() {
        assertThatThrownBy(() -> service.create(20L, 3L, request(InquiryType.WRONG_OR_DAMAGED, null, List.of())))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INQUIRY_IMAGE_REQUIRED);
    }

    @Test
    void 이미_받았으면_INQ4091_이미_문의중이면_INQ4092() {
        mine.confirmReceipt(NOW, false);
        assertThatThrownBy(() -> service.create(20L, 3L, request(InquiryType.ETC, null, List.of())))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INQUIRY_AFTER_RECEIVED);

        Participation inquiring = Participation.participant(post, member, 4, NOW);
        inquiring.startInquiry();
        given(participationRepository.findByPostIdAndUserId(3L, 20L)).willReturn(Optional.of(inquiring));
        assertThatThrownBy(() -> service.create(20L, 3L, request(InquiryType.ETC, null, List.of())))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INQUIRY_IN_PROGRESS);
    }

    @Test
    void 나눔_진행중이_아니면_POST409() {
        Post shipping = Fixtures.post(4L, host, NOW.plusDays(2));
        given(postRepository.findByIdForUpdate(4L)).willReturn(Optional.of(shipping));
        given(participationRepository.findByPostIdAndUserId(4L, 20L))
                .willReturn(Optional.of(Participation.participant(shipping, member, 1, NOW)));

        assertThatThrownBy(() -> service.create(20L, 4L, request(InquiryType.ETC, null, List.of())))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_POST_STATUS);
    }

    @Test
    void 목록은_호스트는_전체_참여자는_자기_것만() {
        service.list(10L, 3L);
        verify(inquiryRepository).findByPostId(3L);

        service.list(20L, 3L);
        verify(inquiryRepository).findByPostIdAndUserId(3L, 20L);

        assertThatThrownBy(() -> service.list(99L, 3L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.NOT_PARTICIPANT);
    }

    private static InquiryCreateRequest request(InquiryType type, Integer receivedUnits, List<String> images) {
        return new InquiryCreateRequest(type, receivedUnits, DesiredResolution.REDELIVER, "문의합니다", images);
    }
}
