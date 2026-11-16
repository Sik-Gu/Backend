package Sikgu.com.example.Web.domain.participation.controller;

import Sikgu.com.example.Web.domain.participation.service.DistributionPlaceService;
import Sikgu.com.example.Web.domain.post.dto.request.DistributionPlaceRequest;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.DistributionPlaceInfo;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 나눔 장소 합의 API (주문 전). 합의 현황은 공구 상세(distributionPlace)에서도 볼 수 있다.
 * 의견이 맞지 않으면 DELETE /api/posts/{postId}/participations/me?reason=PLACE_DISAGREEMENT 로 나간다.
 */
@RestController
@RequestMapping("/api/posts/{postId}/distribution-place")
@RequiredArgsConstructor
public class DistributionPlaceController {

    private final DistributionPlaceService distributionPlaceService;

    /** 나눔 장소 제안·변경 (호스트). 바꾸면 참여자 동의가 초기화된다 */
    @PutMapping
    public ApiResponse<DistributionPlaceInfo> propose(@AuthenticationPrincipal AuthUser authUser,
                                                      @PathVariable Long postId,
                                                      @RequestBody @Valid DistributionPlaceRequest request) {
        return ApiResponse.onSuccess(distributionPlaceService.propose(authUser.userId(), postId, request));
    }

    /** 제안된 나눔 장소에 동의 (참여자) */
    @PostMapping("/agreement")
    public ApiResponse<DistributionPlaceInfo> agree(@AuthenticationPrincipal AuthUser authUser,
                                                    @PathVariable Long postId) {
        return ApiResponse.onSuccess(distributionPlaceService.agree(authUser.userId(), postId));
    }
}
