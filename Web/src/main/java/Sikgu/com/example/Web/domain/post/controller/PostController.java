package Sikgu.com.example.Web.domain.post.controller;

import Sikgu.com.example.Web.domain.post.dto.request.PostCreateRequest;
import Sikgu.com.example.Web.domain.post.dto.request.PostUpdateRequest;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCancelResponse;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCard;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCreateResponse;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostDetailResponse;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostIdResponse;
import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.post.enums.PostSort;
import Sikgu.com.example.Web.domain.post.service.PostQueryService;
import Sikgu.com.example.Web.domain.post.service.PostService;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.apiPayload.PageResponse;
import Sikgu.com.example.Web.global.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공구 게시글 API (docs/API.md 6장)
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostQueryService postQueryService;

    /**
     * 공구 목록·검색·카테고리·거리·정렬.
     * radius: 내 집 ↔ 글 올린 사람의 집 거리 500 또는 1000(m).
     * keyword: 정확한 단어(제목·상품명) 또는 초성(제목)만.
     */
    @GetMapping
    public ApiResponse<PageResponse<PostCard>> search(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) @Size(max = 50, message = "검색어는 50자 이하입니다.") String keyword,
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "1000") int radius,
            @RequestParam(defaultValue = "true") boolean recruitingOnly,
            @RequestParam(defaultValue = "LATEST") PostSort sort,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상입니다.") int page,
            @RequestParam(defaultValue = "9") @Min(value = 1, message = "size는 1 이상입니다.")
            @Max(value = 50, message = "size는 50 이하입니다.") int size) {
        return ApiResponse.onSuccess(postQueryService.search(authUser.userId(), keyword, category, radius,
                recruitingOnly, sort, page, size));
    }

    /** 공구 등록 (채팅방 자동 개설, 호스트 몫 결제) */
    @PostMapping
    public ApiResponse<PostCreateResponse> create(@AuthenticationPrincipal AuthUser authUser,
                                                  @RequestBody @Valid PostCreateRequest request) {
        return ApiResponse.onSuccess(postService.create(authUser.userId(), request));
    }

    /** 공구 상세 (조회수 +1) */
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> getDetail(@AuthenticationPrincipal AuthUser authUser,
                                                     @PathVariable Long postId) {
        return ApiResponse.onSuccess(postService.getDetail(authUser.userId(), postId));
    }

    /** 공구 수정 (호스트, 모집중 + 다른 참여자 없음) */
    @PatchMapping("/{postId}")
    public ApiResponse<PostIdResponse> update(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long postId,
                                              @RequestBody @Valid PostUpdateRequest request) {
        return ApiResponse.onSuccess(postService.update(authUser.userId(), postId, request));
    }

    /** 공구 취소 (호스트, 주문완료 전). 호스트 포함 전원 환급 */
    @DeleteMapping("/{postId}")
    public ApiResponse<PostCancelResponse> cancel(@AuthenticationPrincipal AuthUser authUser,
                                                  @PathVariable Long postId) {
        return ApiResponse.onSuccess(postService.cancel(authUser.userId(), postId));
    }
}
