package Sikgu.com.example.Web.domain.report.dto.request;

import Sikgu.com.example.Web.domain.report.enums.ReportEnums.ReportReason;
import Sikgu.com.example.Web.domain.report.enums.ReportEnums.ViolationStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** POST /api/reports. 신고 대상은 사람만 (게시글은 받지 않음) */
public record ReportCreateRequest(
        @NotNull(message = "신고 대상은 필수입니다.")
        Long targetUserId,

        @NotNull(message = "신고 사유는 필수입니다.")
        ReportReason reason,

        Detail detail,

        /** ETC면 필수 */
        @Size(max = 1000, message = "신고 내용은 1,000자 이하입니다.")
        String content,

        @Size(max = 5, message = "사진은 5장까지 넣을 수 있습니다.")
        List<@NotBlank(message = "이미지 URL이 비었습니다.") @Size(max = 500) String> imageUrls
) {

    /**
     * @param messageIds     CHAT_ABUSE면 1개 이상
     * @param violationStage TRADE_VIOLATION이면 필수
     */
    public record Detail(List<@NotNull Long> messageIds, ViolationStage violationStage) {
    }
}
