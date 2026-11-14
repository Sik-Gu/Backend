package Sikgu.com.example.Web.domain.user.controller;

import Sikgu.com.example.Web.domain.user.dto.request.LocationRequest;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.LocationResponse;
import Sikgu.com.example.Web.domain.user.service.UserService;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 API (docs/API.md 4장)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 동네 변경 (로그인 필요) */
    @PatchMapping("/me/location")
    public ApiResponse<LocationResponse> changeLocation(@AuthenticationPrincipal AuthUser authUser,
                                                        @RequestBody @Valid LocationRequest request) {
        return ApiResponse.onSuccess(userService.changeLocation(authUser.userId(), request));
    }
}
