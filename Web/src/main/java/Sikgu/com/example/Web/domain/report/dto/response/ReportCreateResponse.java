package Sikgu.com.example.Web.domain.report.dto.response;

import Sikgu.com.example.Web.domain.report.enums.ReportEnums.ReportStatus;

/** POST /api/reports */
public record ReportCreateResponse(Long reportId, ReportStatus status) {
}
