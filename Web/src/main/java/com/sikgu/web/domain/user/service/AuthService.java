package com.sikgu.web.domain.user.service;

import com.sikgu.web.domain.user.dto.CheckLoginIdResponse;
import com.sikgu.web.domain.user.dto.SignupRequest;
import com.sikgu.web.domain.user.dto.SignupResponse;
import com.sikgu.web.domain.user.entity.User;
import com.sikgu.web.domain.user.repository.UserRepository;
import com.sikgu.web.global.exception.ErrorCode;
import com.sikgu.web.global.exception.GeneralException;
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
}