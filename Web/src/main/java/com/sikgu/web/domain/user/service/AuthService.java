package com.sikgu.web.domain.user.service;

import com.sikgu.web.domain.user.dto.LoginRequest;
import com.sikgu.web.domain.user.dto.LoginResponse;
import com.sikgu.web.domain.user.dto.SignupRequest;
import com.sikgu.web.domain.user.dto.SignupResponse;
import com.sikgu.web.domain.user.entity.User;
import com.sikgu.web.domain.user.repository.UserRepository;
import com.sikgu.web.global.exception.ErrorCode;
import com.sikgu.web.global.exception.GeneralException;
import com.sikgu.web.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NicknameGenerator nicknameGenerator;
    private final JwtProvider jwtProvider;

    // 회원가입
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsBySchoolEmail(email)) {
            throw new GeneralException(ErrorCode.EMAIL_DUPLICATED);
        }

        SignupRequest.LocationRequest location = request.location();
        User user = User.builder()
                .schoolEmail(email)
                .password(passwordEncoder.encode(request.password()))
                .nickname(nicknameGenerator.generate())
                .dongName(location.dongName())
                .latitude(location.latitude())
                .longitude(location.longitude())
                .build();

        User savedUser = userRepository.save(user);
        return new SignupResponse(savedUser.getId(), savedUser.getNickname());
    }

    // 로그인
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findBySchoolEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new GeneralException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new GeneralException(ErrorCode.LOGIN_FAILED);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId());
        return new LoginResponse(user.getId(), user.getNickname(), accessToken);
    }

    // 이메일은 소문자로 통일해서 저장·조회
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}