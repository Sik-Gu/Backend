package com.sikgu.domain.user.dto;

public record LoginResponse(Long userId, String nickname, String accessToken) {}
