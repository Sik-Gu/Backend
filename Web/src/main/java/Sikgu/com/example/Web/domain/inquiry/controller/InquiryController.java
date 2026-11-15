package Sikgu.com.example.Web.domain.inquiry.controller;

import Sikgu.com.example.Web.domain.inquiry.dto.request.InquiryCreateRequest;
import Sikgu.com.example.Web.domain.inquiry.dto.response.InquiryResponses.InquiryCreateResponse;
import Sikgu.com.example.Web.domain.inquiry.dto.response.InquiryResponses.InquiryItem;
import Sikgu.com.example.Web.domain.inquiry.service.InquiryService;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.security.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 수령 문의 API (docs/API.md 7장)
 */
@RestController
@RequestMapping("/api/posts/{postId}/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    /** 수령 문제 문의 (참여자) */
    @PostMapping
    public ApiResponse<InquiryCreateResponse> create(@AuthenticationPrincipal AuthUser authUser,
                                                     @PathVariable Long postId,
                                                     @RequestBody @Valid InquiryCreateRequest request) {
        return ApiResponse.onSuccess(inquiryService.create(authUser.userId(), postId, request));
    }

    /** 문의 목록 (호스트: 전체, 참여자: 내 문의) */
    @GetMapping
    public ApiResponse<List<InquiryItem>> list(@AuthenticationPrincipal AuthUser authUser,
                                               @PathVariable Long postId) {
        return ApiResponse.onSuccess(inquiryService.list(authUser.userId(), postId));
    }
}
