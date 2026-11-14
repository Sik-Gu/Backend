package Sikgu.com.example.Web.domain.user.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 동네 정보. 동 이름 검색이든 GPS든 같은 형식으로 받는다 (docs/DB.md D14).
 * 회원가입, 동네 변경에서 같이 쓴다.
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
