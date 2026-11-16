package Sikgu.com.example.Web.domain.report.service;

import Sikgu.com.example.Web.domain.report.dto.request.ReportCreateRequest;
import Sikgu.com.example.Web.domain.report.dto.response.ReportCreateResponse;
import Sikgu.com.example.Web.domain.report.entity.Report;
import Sikgu.com.example.Web.domain.report.enums.ReportEnums.ReportReason;
import Sikgu.com.example.Web.domain.report.enums.ReportEnums.ViolationStage;
import Sikgu.com.example.Web.domain.report.repository.ReportRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 사용자 신고 (docs/API.md 10장). 신고 대상은 사람만 (게시글 관련 신고 없음).
 * 처리 전(PENDING)으로 저장만 한다. 처리는 운영팀 (관리자 화면은 나중에).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReportCreateResponse create(Long reporterId, ReportCreateRequest request) {
        if (reporterId.equals(request.targetUserId())) {
            throw new GeneralException(ErrorStatus.CANNOT_REPORT_SELF);
        }
        List<Long> messageIds = request.detail() == null ? null : request.detail().messageIds();
        ViolationStage stage = request.detail() == null ? null : request.detail().violationStage();
        validateByReason(request, messageIds, stage);

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        User target = userRepository.findById(request.targetUserId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Report report = reportRepository.save(Report.create(reporter, target, request.reason(), stage,
                request.content(), messageIds, request.imageUrls()));
        return new ReportCreateResponse(report.getId(), report.getStatus());
    }

    /** 사유별 필수 값 (REPORT4002) */
    private static void validateByReason(ReportCreateRequest request, List<Long> messageIds, ViolationStage stage) {
        ReportReason reason = request.reason();
        boolean valid = switch (reason) {
            case CHAT_ABUSE -> messageIds != null && !messageIds.isEmpty();
            case TRADE_VIOLATION -> stage != null;
            case ETC -> StringUtils.hasText(request.content());
        };
        if (!valid) {
            throw new GeneralException(ErrorStatus.REPORT_DETAIL_REQUIRED);
        }
    }
}
