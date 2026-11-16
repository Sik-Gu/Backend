package com.sikgu.domain.user.dto;

import com.sikgu.domain.user.entity.User;

import java.time.LocalDateTime;

public record MyInfoResponse(
        Long userId, String schoolEmail, String nickname, String dongName, LocalDateTime createdAt
) {
    public static MyInfoResponse from(User user) {
        return new MyInfoResponse(
                user.getId(), user.getSchoolEmail(), user.getNickname(),
                user.getDongName(), user.getCreatedAt());
    }
}
