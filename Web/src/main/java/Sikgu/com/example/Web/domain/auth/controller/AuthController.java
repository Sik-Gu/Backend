package Sikgu.com.example.Web.domain.auth.controller;

import Sikgu.com.example.Web.domain.auth.dto.request.AuthPatterns;
import Sikgu.com.example.Web.domain.auth.dto.request.EmailSendRequest;
import Sikgu.com.example.Web.domain.auth.dto.request.EmailVerifyRequest;
import Sikgu.com.example.Web.domain.auth.dto.request.SignupRequest;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.CheckIdResponse;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.EmailSendResponse;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.EmailVerifyResponse;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.SignupResponse;
import Sikgu.com.example.Web.domain.auth.service.AuthService;
import Sikgu.com.example.Web.domain.auth.service.EmailVerificationService;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입 3단계: 이메일 인증(send → verify) → 아이디 확인 → signup
 * docs/API.md 4장 참고
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    /** 학교 이메일 인증코드 발송 */
    @PostMapping("/email/send")
    public ApiResponse<EmailSendResponse> sendEmailCode(@RequestBody @Valid EmailSendRequest request) {
        return ApiResponse.onSuccess(emailVerificationService.sendCode(request.email()));
    }

    /** 인증코드 확인 → verificationToken 발급 */
    @PostMapping("/email/verify")
    public ApiResponse<EmailVerifyResponse> verifyEmailCode(@RequestBody @Valid EmailVerifyRequest request) {
        return ApiResponse.onSuccess(emailVerificationService.verifyCode(request.email(), request.code()));
    }

    /** 아이디 중복확인. 중복이어도 200 + available=false */
    @GetMapping("/check-id")
    public ApiResponse<CheckIdResponse> checkLoginId(
            @RequestParam @Pattern(regexp = AuthPatterns.LOGIN_ID, message = AuthPatterns.LOGIN_ID_MESSAGE)
            String loginId) {
        return ApiResponse.onSuccess(authService.checkLoginId(loginId));
    }

    /** 회원가입 (닉네임 랜덤, 포인트 지갑 생성) */
    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@RequestBody @Valid SignupRequest request) {
        return ApiResponse.onSuccess(authService.signup(request));
    }
}
