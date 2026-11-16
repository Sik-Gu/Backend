package com.sikgu.web.domain.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "학교 이메일을 입력해주세요")
        @Email(message = "이메일 형식이 아니에요")
        @Pattern(
                regexp = "^[^@]+@([A-Za-z0-9-]+\\.)+ac\\.kr$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "학교 이메일(ac.kr)만 가입할 수 있어요"
        )
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자로 입력해주세요")
        String password,

        @NotNull(message = "동네를 설정해주세요")
        @Valid
        LocationRequest location
) {
    public record LocationRequest(
            @NotBlank(message = "동네 이름을 입력해주세요")
            String dongName,

            @NotNull(message = "위도를 입력해주세요")
            Double latitude,

            @NotNull(message = "경도를 입력해주세요")
            Double longitude
    ) {
    }
}