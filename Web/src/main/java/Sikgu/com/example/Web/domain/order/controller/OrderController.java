package Sikgu.com.example.Web.domain.order.controller;

import Sikgu.com.example.Web.domain.order.dto.request.OrderRequests.DistributionRequest;
import Sikgu.com.example.Web.domain.order.dto.request.OrderRequests.OrderRegisterRequest;
import Sikgu.com.example.Web.domain.order.dto.request.OrderRequests.ShippingRequest;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.DistributionResponse;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.OrderDetailResponse;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.OrderRegisterResponse;
import Sikgu.com.example.Web.domain.order.dto.response.OrderResponses.ShippingResponse;
import Sikgu.com.example.Web.domain.order.service.OrderService;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문·배송·나눔 API (docs/API.md 7장)
 */
@RestController
@RequestMapping("/api/posts/{postId}/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 영수증 등록 + 주문완료 + 차액 정산 (호스트) */
    @PostMapping
    public ApiResponse<OrderRegisterResponse> register(@AuthenticationPrincipal AuthUser authUser,
                                                       @PathVariable Long postId,
                                                       @RequestBody @Valid OrderRegisterRequest request) {
        return ApiResponse.onSuccess(orderService.register(authUser.userId(), postId, request));
    }

    /** 주문 정보·영수증·차액 조회 (호스트·참여자) */
    @GetMapping
    public ApiResponse<OrderDetailResponse> get(@AuthenticationPrincipal AuthUser authUser,
                                                @PathVariable Long postId) {
        return ApiResponse.onSuccess(orderService.get(authUser.userId(), postId));
    }

    /** 택배사·송장 + 배송 시작 (호스트) */
    @PatchMapping("/shipping")
    public ApiResponse<ShippingResponse> ship(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long postId,
                                              @RequestBody @Valid ShippingRequest request) {
        return ApiResponse.onSuccess(orderService.ship(authUser.userId(), postId, request));
    }

    /** 나눔 일정·장소 입력 → 나눔 진행 (호스트, D6) */
    @PatchMapping("/distribution")
    public ApiResponse<DistributionResponse> scheduleDistribution(@AuthenticationPrincipal AuthUser authUser,
                                                                  @PathVariable Long postId,
                                                                  @RequestBody @Valid DistributionRequest request) {
        return ApiResponse.onSuccess(orderService.scheduleDistribution(authUser.userId(), postId, request));
    }
}
