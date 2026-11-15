package com.sikgu.web.domain.user.controller;

import com.sikgu.web.domain.user.dto.CheckLoginIdResponse;
import com.sikgu.web.domain.user.service.AuthService;
import com.sikgu.web.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/check-id")
    public ApiResponse<CheckLoginIdResponse> checkLoginId(@RequestParam String loginId) {
        return ApiResponse.onSuccess(authService.checkLoginId(loginId));
    }
}