package Sikgu.com.example.Web.domain.inquiry.dto.request;

import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.DesiredResolution;
import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.InquiryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** POST /api/posts/{postId}/inquiries */
public record InquiryCreateRequest(
        @NotNull(message = "문의 유형은 필수입니다.")
        InquiryType type,

        /** SHORT_QUANTITY면 필수 */
        Integer receivedUnits,

        DesiredResolution desiredResolution,

        @NotBlank(message = "문의 내용은 필수입니다.")
        @Size(max = 1000, message = "문의 내용은 1,000자 이하입니다.")
        String content,

        /** WRONG_OR_DAMAGED면 1장 이상 */
        @Size(max = 5, message = "사진은 5장까지 넣을 수 있습니다.")
        List<@NotBlank(message = "이미지 URL이 비었습니다.") @Size(max = 500) String> imageUrls
) {
}
