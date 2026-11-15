package Sikgu.com.example.Web.domain.post.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 거래 장소. 동 이름은 프론트가 카카오맵 SDK로 구해서 보낸다 (D15).
 */
public record TradePlaceRequest(
        @NotBlank(message = "거래 장소 이름은 필수입니다.")
        @Size(max = 100, message = "거래 장소 이름은 100자 이하입니다.")
        String name,

        @NotBlank(message = "거래 장소의 동 이름은 필수입니다.")
        @Size(max = 30, message = "동 이름은 30자 이하입니다.")
        String dongName,

        @NotNull(message = "거래 장소 위도는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상입니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하입니다.")
        Double latitude,

        @NotNull(message = "거래 장소 경도는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상입니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하입니다.")
        Double longitude
) {
}
