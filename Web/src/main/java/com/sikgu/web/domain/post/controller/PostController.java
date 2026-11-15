package com.sikgu.web.domain.post.controller;

import com.sikgu.web.domain.post.dto.CreatePostRequest;
import com.sikgu.web.domain.post.dto.CreatePostResponse;
import com.sikgu.web.domain.post.dto.PostDetailResponse;
import com.sikgu.web.domain.post.dto.UpdatePostRequest;
import com.sikgu.web.domain.post.service.PostService;
import com.sikgu.web.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    public ApiResponse<CreatePostResponse> createPost(@AuthenticationPrincipal Long userId,
                                                      @Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.onSuccess("공구 등록 성공", postService.createPost(userId, request));
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> getPost(@PathVariable Long postId) {
        return ApiResponse.onSuccess(postService.getPost(postId));
    }

    @PatchMapping("/{postId}")
    public ApiResponse<Void> updatePost(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long postId,
                                        @Valid @RequestBody UpdatePostRequest request) {
        postService.updatePost(userId, postId, request);
        return ApiResponse.onSuccess("공구 수정 성공", null);
    }
}