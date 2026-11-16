package Sikgu.com.example.Web.domain.participation.controller;

import Sikgu.com.example.Web.domain.participation.dto.request.ParticipationJoinRequest;
import Sikgu.com.example.Web.domain.participation.dto.response.ParticipationResponses.JoinResponse;
import Sikgu.com.example.Web.domain.participation.dto.response.ParticipationResponses.LeaveResponse;
import Sikgu.com.example.Web.domain.participation.dto.response.ParticipationResponses.ParticipationStatusResponse;
import Sikgu.com.example.Web.domain.participation.enums.LeaveReason;
import Sikgu.com.example.Web.domain.participation.service.ParticipationService;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공구 참여 API (docs/API.md 7장)
 */
@RestController
@RequestMapping("/api/posts/{postId}/participations")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;

    /** 참여 + 즉시 결제(보류) */
    @PostMapping
    public ApiResponse<JoinResponse> join(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long postId,
                                          @RequestBody @Valid ParticipationJoinRequest request) {
        return ApiResponse.onSuccess(participationService.join(authUser.userId(), postId, request.units()));
    }

    /**
     * 참여 취소(나가기) + 전액 환급. 주문 전에만.
     * 나눔 장소 의견이 맞지 않아 나가는 경우 reason=PLACE_DISAGREEMENT
     */
    @DeleteMapping("/me")
    public ApiResponse<LeaveResponse> leave(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long postId,
                                            @RequestParam(defaultValue = "CHANGED_MIND") LeaveReason reason) {
        return ApiResponse.onSuccess(participationService.leave(authUser.userId(), postId, reason));
    }

    /** 결제·수령 현황 (호스트·참여자) */
    @GetMapping
    public ApiResponse<ParticipationStatusResponse> getStatus(@AuthenticationPrincipal AuthUser authUser,
                                                              @PathVariable Long postId) {
        return ApiResponse.onSuccess(participationService.getStatus(authUser.userId(), postId));
    }
}
