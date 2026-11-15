package com.sikgu.web.domain.user.service;

import com.sikgu.web.domain.user.dto.*;
import com.sikgu.web.domain.user.entity.User;
import com.sikgu.web.domain.user.repository.UserRepository;
import com.sikgu.web.global.exception.ErrorCode;
import com.sikgu.web.global.exception.GeneralException;
import com.sikgu.web.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NicknameGenerator nicknameGenerator;
    private final JwtProvider jwtProvider;

    public CheckLoginIdResponse checkLoginId(String loginId) {
        boolean available = !userRepository.existsByLoginId(loginId);
        return new CheckLoginIdResponse(loginId, available);
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new GeneralException(ErrorCode.LOGIN_ID_DUPLICATED);
        }

        SignupRequest.LocationRequest location = request.location();
        User user = User.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .nickname(nicknameGenerator.generate())
                .dongName(location.dongName())
                .latitude(location.latitude())
                .longitude(location.longitude())
                .build();

        User savedUser = userRepository.save(user);
        return new SignupResponse(savedUser.getId(), savedUser.getNickname());
    }

    public LoginResponse login(LoginRequest request) {
        // 1. 아이디로 사용자 찾기 (없으면 실패)
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new GeneralException(ErrorCode.LOGIN_FAILED));

        // 2. 비밀번호 비교 (틀리면 실패)
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new GeneralException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 토큰 발급
        String accessToken = jwtProvider.createAccessToken(user.getId());
        return new LoginResponse(user.getId(), user.getNickname(), accessToken);
    }
}