package Sikgu.com.example.Web.domain.review.controller;

import Sikgu.com.example.Web.domain.review.dto.request.ReviewWriteRequest;
import Sikgu.com.example.Web.domain.review.dto.response.ReviewResponses.ReceivedReview;
import Sikgu.com.example.Web.domain.review.dto.response.ReviewResponses.ReviewTargetsResponse;
import Sikgu.com.example.Web.domain.review.dto.response.ReviewResponses.ReviewWriteResponse;
import Sikgu.com.example.Web.domain.review.service.ReviewService;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.apiPayload.PageResponse;
import Sikgu.com.example.Web.global.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 후기 API (docs/API.md 10장)
 */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 평가 대상·작성 여부 (호스트·참여자) */
    @GetMapping("/api/posts/{postId}/reviews/targets")
    public ApiResponse<ReviewTargetsResponse> getTargets(@AuthenticationPrincipal AuthUser authUser,
                                                         @PathVariable Long postId) {
        return ApiResponse.onSuccess(reviewService.getTargets(authUser.userId(), postId));
    }

    /** 후기 작성 (배열). 정산 후 3일 이내, 수정 불가 (D12) */
    @PostMapping("/api/posts/{postId}/reviews")
    public ApiResponse<ReviewWriteResponse> write(@AuthenticationPrincipal AuthUser authUser,
                                                  @PathVariable Long postId,
                                                  @RequestBody @Valid ReviewWriteRequest request) {
        return ApiResponse.onSuccess(reviewService.write(authUser.userId(), postId, request));
    }

    /** 받은 후기 목록 */
    @GetMapping("/api/users/{userId}/reviews")
    public ApiResponse<PageResponse<ReceivedReview>> getReceived(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상입니다.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1 이상입니다.")
            @Max(value = 50, message = "size는 50 이하입니다.") int size) {
        return ApiResponse.onSuccess(reviewService.getReceived(userId, page, size));
    }
}
