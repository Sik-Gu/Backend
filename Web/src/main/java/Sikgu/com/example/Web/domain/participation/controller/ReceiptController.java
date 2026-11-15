package Sikgu.com.example.Web.domain.participation.controller;

import Sikgu.com.example.Web.domain.participation.service.ReceiptService;
import Sikgu.com.example.Web.domain.participation.service.ReceiptService.ReceiptResult;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 받았어요 (docs/API.md 7장)
 */
@RestController
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    /** 받았어요 (참여자). 마지막 사람이면 정산완료 */
    @PostMapping("/api/posts/{postId}/receipt")
    public ApiResponse<ReceiptResult> confirm(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long postId) {
        return ApiResponse.onSuccess(receiptService.confirm(authUser.userId(), postId));
    }
}
