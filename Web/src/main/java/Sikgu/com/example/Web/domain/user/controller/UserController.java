package Sikgu.com.example.Web.domain.user.controller;

import Sikgu.com.example.Web.domain.user.dto.request.LocationRequest;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.LocationResponse;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.MeResponse;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.MyPostCard;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.ProfileResponse;
import Sikgu.com.example.Web.domain.user.enums.MyPostEnums.MyPostFilter;
import Sikgu.com.example.Web.domain.user.enums.MyPostEnums.MyPostTab;
import Sikgu.com.example.Web.domain.user.service.UserQueryService;
import Sikgu.com.example.Web.domain.user.service.UserService;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.apiPayload.PageResponse;
import Sikgu.com.example.Web.global.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 API (docs/API.md 4장)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserQueryService userQueryService;

    /** 내 정보: 프로필 + 공구 수 + 포인트 */
    @GetMapping("/me")
    public ApiResponse<MeResponse> getMe(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.onSuccess(userQueryService.getMe(authUser.userId()));
    }

    /** 집 위치(GPS) 설정·변경. 가입 직후 한 번 설정해야 공구 목록·등록을 쓸 수 있다 */
    @PatchMapping("/me/location")
    public ApiResponse<LocationResponse> changeLocation(@AuthenticationPrincipal AuthUser authUser,
                                                        @RequestBody @Valid LocationRequest request) {
        return ApiResponse.onSuccess(userService.changeLocation(authUser.userId(), request));
    }

    /** 내 공구함 */
    @GetMapping("/me/posts")
    public ApiResponse<PageResponse<MyPostCard>> getMyPosts(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "ONGOING") MyPostTab tab,
            @RequestParam(defaultValue = "ALL") MyPostFilter filter,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상입니다.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1 이상입니다.")
            @Max(value = 50, message = "size는 50 이하입니다.") int size) {
        return ApiResponse.onSuccess(userQueryService.getMyPosts(authUser.userId(), tab, filter, page, size));
    }

    /** 유저 프로필 모달 */
    @GetMapping("/{userId}")
    public ApiResponse<ProfileResponse> getProfile(@AuthenticationPrincipal AuthUser authUser,
                                                   @PathVariable Long userId) {
        return ApiResponse.onSuccess(userQueryService.getProfile(authUser.userId(), userId));
    }
}
