package com.sikgu.web.domain.post.dto;

import com.sikgu.web.domain.post.entity.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// 바꿀 필드만 보냄 (안 보낸 필드는 null → 그대로 유지)
public record UpdatePostRequest(
        @Size(min = 1, max = 100, message = "제목은 1~100자로 입력해주세요")
        String title,

        @Size(min = 1, max = 100, message = "상품 이름은 1~100자로 입력해주세요")
        String productName,

        @Size(max = 500, message = "상품 링크는 500자 이하로 입력해주세요")
        String productLink,

        Category category,

        @Size(min = 1, message = "내용을 입력해주세요")
        String content,

        @Future(message = "마감 일시는 현재 이후여야 해요")
        LocalDateTime deadlineAt,

        @Valid
        TradePlaceRequest tradePlace
) {
    // 거래 장소는 보낼 거면 세 값 다
    public record TradePlaceRequest(
            @NotBlank(message = "거래 장소 이름을 입력해주세요")
            String name,

            @NotNull(message = "위도를 입력해주세요")
            Double latitude,

            @NotNull(message = "경도를 입력해주세요")
            Double longitude
    ) {
    }
}