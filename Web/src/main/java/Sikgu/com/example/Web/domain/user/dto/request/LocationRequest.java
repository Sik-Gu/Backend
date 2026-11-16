package Sikgu.com.example.Web.domain.user.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 집 위치. 프론트가 기기 GPS로 위경도를 구하고, 카카오맵 SDK로 동 이름을 구해서 보낸다.
 * PATCH /api/users/me/location 에서 쓴다.
 */
public record LocationRequest(
        @NotBlank(message = "동 이름은 필수입니다.")
        @Size(max = 30, message = "동 이름은 30자 이하입니다.")
        String dongName,

        @NotNull(message = "위도는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상입니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하입니다.")
        Double latitude,

        @NotNull(message = "경도는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상입니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하입니다.")
        Double longitude
) {
}
