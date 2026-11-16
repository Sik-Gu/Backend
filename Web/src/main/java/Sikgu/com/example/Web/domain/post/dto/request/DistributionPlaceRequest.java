package Sikgu.com.example.Web.domain.post.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/posts/{postId}/distribution-place. 호스트가 나눔 장소를 제안·변경한다.
 * 나눔 장소는 공개 장소(예: 학교 정문, 편의점 앞)라서 좌표를 보내면 지도에 표시한다. 좌표는 선택.
 */
public record DistributionPlaceRequest(
        @NotBlank(message = "나눔 장소 이름은 필수입니다.")
        @Size(max = 100, message = "나눔 장소 이름은 100자 이하입니다.")
        String name,

        @DecimalMin(value = "-90.0", message = "위도는 -90 이상입니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하입니다.")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "경도는 -180 이상입니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하입니다.")
        Double longitude
) {
    /** 위도와 경도는 둘 다 보내거나 둘 다 비운다 */
    @AssertTrue(message = "위도와 경도는 함께 입력해야 합니다.")
    public boolean isCoordinateBothOrNone() {
        return (latitude == null) == (longitude == null);
    }
}
