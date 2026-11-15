package Sikgu.com.example.Web.domain.block.controller;

import Sikgu.com.example.Web.domain.block.dto.response.BlockResponses.BlockResponse;
import Sikgu.com.example.Web.domain.block.dto.response.BlockResponses.BlockedUser;
import Sikgu.com.example.Web.domain.block.service.BlockService;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.apiPayload.PageResponse;
import Sikgu.com.example.Web.global.security.AuthUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 차단 API (docs/API.md 10장)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @PostMapping("/{userId}/block")
    public ApiResponse<BlockResponse> block(@AuthenticationPrincipal AuthUser authUser,
                                            @PathVariable Long userId) {
        return ApiResponse.onSuccess(blockService.block(authUser.userId(), userId));
    }

    @DeleteMapping("/{userId}/block")
    public ApiResponse<Void> unblock(@AuthenticationPrincipal AuthUser authUser,
                                     @PathVariable Long userId) {
        blockService.unblock(authUser.userId(), userId);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("/me/blocks")
    public ApiResponse<PageResponse<BlockedUser>> getMyBlocks(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상입니다.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1 이상입니다.")
            @Max(value = 50, message = "size는 50 이하입니다.") int size) {
        return ApiResponse.onSuccess(blockService.getMyBlocks(authUser.userId(), page, size));
    }
}
