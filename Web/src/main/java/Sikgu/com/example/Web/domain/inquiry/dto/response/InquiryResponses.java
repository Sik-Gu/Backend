package Sikgu.com.example.Web.domain.inquiry.dto.response;

import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.DesiredResolution;
import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.InquiryResolution;
import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.InquiryStatus;
import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.InquiryType;
import Sikgu.com.example.Web.domain.participation.enums.ReceiptStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 수령 문의 API 응답 DTO 모음 (docs/API.md 7장).
 */
public final class InquiryResponses {

    /** POST /api/posts/{postId}/inquiries */
    public record InquiryCreateResponse(Long inquiryId, ReceiptStatus receiptStatus, InquiryStatus inquiryStatus) {
    }

    /** GET /api/posts/{postId}/inquiries 항목 */
    public record InquiryItem(
            Long inquiryId,
            Long userId,
            String nickname,
            InquiryType type,
            Integer receivedUnits,
            DesiredResolution desiredResolution,
            String content,
            List<String> imageUrls,
            InquiryStatus status,
            InquiryResolution resolution,
            Integer refundAmount,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt
    ) {
    }

    private InquiryResponses() {
    }
}
