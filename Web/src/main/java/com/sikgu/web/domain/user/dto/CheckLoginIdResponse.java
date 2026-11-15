package com.sikgu.web.domain.user.dto;

public record CheckLoginIdResponse(String loginId, boolean available) {
}