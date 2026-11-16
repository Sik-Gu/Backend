package Sikgu.com.example.Web.domain.report.controller;

import Sikgu.com.example.Web.domain.report.dto.request.ReportCreateRequest;
import Sikgu.com.example.Web.domain.report.dto.response.ReportCreateResponse;
import Sikgu.com.example.Web.domain.report.service.ReportService;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 신고 API (docs/API.md 10장)
 */
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 사용자 신고 */
    @PostMapping("/api/reports")
    public ApiResponse<ReportCreateResponse> create(@AuthenticationPrincipal AuthUser authUser,
                                                    @RequestBody @Valid ReportCreateRequest request) {
        return ApiResponse.onSuccess(reportService.create(authUser.userId(), request));
    }
}
