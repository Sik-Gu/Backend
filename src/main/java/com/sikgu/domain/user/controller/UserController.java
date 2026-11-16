package com.sikgu.domain.user.controller;

import com.sikgu.domain.user.dto.MyInfoResponse;
import com.sikgu.domain.user.service.UserService;
import com.sikgu.global.apiPayload.ApiResponse;
import com.sikgu.global.auth.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo(
            @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        return ResponseEntity.ok(
                ApiResponse.ok("내 정보를 조회했습니다.", userService.getMyInfo(userId)));
    }
}
