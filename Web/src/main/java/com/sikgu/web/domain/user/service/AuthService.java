package com.sikgu.web.domain.user.service;

import com.sikgu.web.domain.user.dto.CheckLoginIdResponse;
import com.sikgu.web.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;

    public CheckLoginIdResponse checkLoginId(String loginId) {
        boolean available = !userRepository.existsByLoginId(loginId);
        return new CheckLoginIdResponse(loginId, available);
    }
}