package com.sikgu.web.domain.user.controller;

import com.sikgu.web.domain.user.dto.CheckLoginIdResponse;
import com.sikgu.web.domain.user.dto.SignupRequest;
import com.sikgu.web.domain.user.dto.SignupResponse;
import com.sikgu.web.domain.user.service.AuthService;
import com.sikgu.web.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/check-id")
    public ApiResponse<CheckLoginIdResponse> checkLoginId(@RequestParam String loginId) {
        return ApiResponse.onSuccess(authService.checkLoginId(loginId));
    }

    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.onSuccess("회원가입 성공", authService.signup(request));
    }
}