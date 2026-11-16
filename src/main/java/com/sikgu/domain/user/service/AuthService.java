package com.sikgu.domain.user.service;

import com.sikgu.domain.user.dto.*;
import com.sikgu.domain.user.entity.User;
import com.sikgu.domain.user.nickname.NicknameGenerator;
import com.sikgu.domain.user.repository.UserRepository;
import com.sikgu.global.apiPayload.ApiException;
import com.sikgu.global.auth.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NicknameGenerator nicknameGenerator;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsBySchoolEmail(request.schoolEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "USER409_3", "이미 가입된 이메일입니다.");
        }

        String nickname = nicknameGenerator.generateUnique();

        User user = User.builder()
                .schoolEmail(request.schoolEmail())
                .password(passwordEncoder.encode(request.password()))
                .nickname(nickname)
                .build();

        User saved = userRepository.save(user);
        return new SignupResponse(saved.getId(), saved.getSchoolEmail(), saved.getNickname());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findBySchoolEmail(request.schoolEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH401_1",
                        "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH401_1",
                    "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtUtil.createAccessToken(user.getId());
        return new LoginResponse(user.getId(), user.getNickname(), accessToken);
    }
}
