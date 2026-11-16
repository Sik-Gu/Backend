package Sikgu.com.example.Web.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.report.dto.request.ReportCreateRequest;
import Sikgu.com.example.Web.domain.report.dto.request.ReportCreateRequest.Detail;
import Sikgu.com.example.Web.domain.report.entity.Report;
import Sikgu.com.example.Web.domain.report.enums.ReportEnums.ReportReason;
import Sikgu.com.example.Web.domain.report.enums.ReportEnums.ReportStatus;
import Sikgu.com.example.Web.domain.report.enums.ReportEnums.ViolationStage;
import Sikgu.com.example.Web.domain.report.repository.ReportRepository;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.support.Fixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** 신고는 사람만 대상으로 한다 (게시글 관련 신고 없음) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {

    @Mock
    ReportRepository reportRepository;
    @Mock
    UserRepository userRepository;

    @InjectMocks
    ReportService service;

    @BeforeEach
    void setUp() {
        given(userRepository.findById(1L)).willReturn(Optional.of(Fixtures.user(1L)));
        given(userRepository.findById(2L)).willReturn(Optional.of(Fixtures.user(2L)));
        given(reportRepository.save(any(Report.class))).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void 거래_위반_신고를_위반_단계와_함께_처리_전으로_저장한다() {
        var response = service.create(1L, new ReportCreateRequest(2L, ReportReason.TRADE_VIOLATION,
                new Detail(List.of(), ViolationStage.DISTRIBUTION), "물건을 받지 못했어요", List.of("/e.jpg")));

        assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getTarget().getId()).isEqualTo(2L);
        assertThat(captor.getValue().getViolationStage()).isEqualTo(ViolationStage.DISTRIBUTION);
        assertThat(captor.getValue().getImageUrls()).containsExactly("/e.jpg");
    }

    @Test
    void 채팅_신고는_메시지_ID가_있으면_된다() {
        service.create(1L, new ReportCreateRequest(2L, ReportReason.CHAT_ABUSE, new Detail(List.of(501L), null),
                null, null));

        verify(reportRepository).save(any());
    }

    @Test
    void 기타_신고는_내용이_있으면_된다() {
        service.create(1L, new ReportCreateRequest(2L, ReportReason.ETC, null, "약속을 계속 어겨요", null));

        verify(reportRepository).save(any());
    }

    @Test
    void 자기_자신은_신고할_수_없다() {
        assertThatThrownBy(() -> service.create(1L, new ReportCreateRequest(1L, ReportReason.ETC, null, "내용", null)))
                .extracting("errorStatus").isEqualTo(ErrorStatus.CANNOT_REPORT_SELF);
    }

    @Test
    void 사유별_필수_값이_없으면_REPORT4002() {
        assertThatThrownBy(() -> service.create(1L,
                new ReportCreateRequest(2L, ReportReason.CHAT_ABUSE, new Detail(List.of(), null), null, null)))
                .extracting("errorStatus").isEqualTo(ErrorStatus.REPORT_DETAIL_REQUIRED);
        assertThatThrownBy(() -> service.create(1L,
                new ReportCreateRequest(2L, ReportReason.TRADE_VIOLATION, null, "내용", null)))
                .extracting("errorStatus").isEqualTo(ErrorStatus.REPORT_DETAIL_REQUIRED);
        assertThatThrownBy(() -> service.create(1L,
                new ReportCreateRequest(2L, ReportReason.ETC, null, "  ", null)))
                .extracting("errorStatus").isEqualTo(ErrorStatus.REPORT_DETAIL_REQUIRED);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void 없는_사용자면_COMMON404() {
        given(userRepository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(1L, new ReportCreateRequest(9L, ReportReason.ETC, null, "내용", null)))
                .extracting("errorStatus").isEqualTo(ErrorStatus.USER_NOT_FOUND);
    }
}
