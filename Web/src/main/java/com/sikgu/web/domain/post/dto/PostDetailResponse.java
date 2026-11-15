package com.sikgu.web.domain.post.dto;

import com.sikgu.web.domain.post.entity.Category;
import com.sikgu.web.domain.post.entity.Post;
import com.sikgu.web.domain.post.entity.PostStatus;
import com.sikgu.web.domain.user.entity.User;

import java.time.LocalDateTime;

public record PostDetailResponse(
        Long postId,
        String title,
        String productName,
        String productLink,
        Category category,
        String content,
        String baseUnit,
        Integer totalUnits,
        Integer filledUnits,
        Integer unitPrice,
        Integer totalPrice,
        Integer maxSelectableUnits,
        PostStatus status,
        String dongName,
        String tradePlaceName,
        LocalDateTime deadlineAt,
        Integer viewCount,
        LocalDateTime createdAt,
        HostResponse host
) {
    public record HostResponse(Long userId, String nickname, Double rating) {
    }

    // Post 엔티티 → 응답 DTO
    public static PostDetailResponse from(Post post) {
        User host = post.getHost();
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getProductName(),
                post.getProductLink(),
                post.getCategory(),
                post.getContent(),
                post.getBaseUnit(),
                post.getTotalUnits(),
                post.getFilledUnits(),
                post.getUnitPrice(),
                post.getTotalPrice(),
                post.getTotalUnits() - post.getFilledUnits(),
                post.getStatus(),
                host.getDongName(),
                post.getTradePlaceName(),
                post.getDeadlineAt(),
                post.getViewCount(),
                post.getCreatedAt(),
                new HostResponse(host.getId(), host.getNickname(), host.getRating())
        );
    }
}