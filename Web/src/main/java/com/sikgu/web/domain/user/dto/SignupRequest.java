package com.sikgu.web.domain.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "아이디를 입력해주세요")
        @Pattern(regexp = "^[a-z0-9]{4,20}$", message = "아이디는 영문 소문자·숫자 4~20자로 입력해주세요")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자로 입력해주세요")
        String password,

        @NotNull(message = "동네를 설정해주세요")
        @Valid
        LocationRequest location
) {
    public record LocationRequest(
            @NotBlank(message = "동 이름을 입력해주세요")
            String dongName,

            @NotNull(message = "위도를 입력해주세요")
            Double latitude,

            @NotNull(message = "경도를 입력해주세요")
            Double longitude
    ) {
    }
}