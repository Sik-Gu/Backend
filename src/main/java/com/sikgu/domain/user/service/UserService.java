package com.sikgu.domain.user.service;

import com.sikgu.domain.user.dto.MyInfoResponse;
import com.sikgu.domain.user.entity.User;
import com.sikgu.domain.user.repository.UserRepository;
import com.sikgu.global.apiPayload.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER404_1",
                        "해당 사용자를 찾을 수 없습니다."));
        return MyInfoResponse.from(user);
    }
}
