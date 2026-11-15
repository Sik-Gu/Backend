package Sikgu.com.example.Web.domain.post.controller;

import Sikgu.com.example.Web.domain.post.dto.request.PostCreateRequest;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCreateResponse;
import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostDetailResponse;
import Sikgu.com.example.Web.domain.post.service.PostService;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공구 게시글 API (docs/API.md 6장)
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

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
}
