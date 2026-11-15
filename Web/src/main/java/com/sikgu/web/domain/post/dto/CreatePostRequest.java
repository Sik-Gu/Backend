package com.sikgu.web.domain.post.dto;

import com.sikgu.web.domain.post.entity.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreatePostRequest(
        @NotBlank(message = "제목을 입력해주세요")
        @Size(max = 100, message = "제목은 100자 이하로 입력해주세요")
        String title,

        @NotBlank(message = "상품 이름을 입력해주세요")
        @Size(max = 100, message = "상품 이름은 100자 이하로 입력해주세요")
        String productName,

        @Size(max = 500, message = "상품 링크는 500자 이하로 입력해주세요")
        String productLink,

        @NotNull(message = "카테고리를 선택해주세요")
        Category category,

        @NotBlank(message = "내용을 입력해주세요")
        String content,

        @NotBlank(message = "기본 단위를 입력해주세요")
        @Size(max = 50, message = "기본 단위는 50자 이하로 입력해주세요")
        String baseUnit,

        @NotNull(message = "총 단위 수를 입력해주세요")
        @Min(value = 2, message = "총 단위 수는 2 이상이어야 해요")
        Integer totalUnits,

        @NotNull(message = "총 가격을 입력해주세요")
        @Positive(message = "총 가격은 0보다 커야 해요")
        Integer totalPrice,

        @NotNull(message = "본인 구매 수량을 입력해주세요")
        @Min(value = 1, message = "본인 구매 수량은 1 이상이어야 해요")
        Integer hostUnits,

        @NotNull(message = "마감 일시를 입력해주세요")
        @Future(message = "마감 일시는 현재 이후여야 해요")
        LocalDateTime deadlineAt,

        @NotNull(message = "거래 장소를 입력해주세요")
        @Valid
        TradePlaceRequest tradePlace
) {
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