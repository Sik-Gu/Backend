package Sikgu.com.example.Web.domain.user.service;

import Sikgu.com.example.Web.domain.user.dto.request.LocationRequest;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.LocationResponse;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 동네 변경. 이후 공구 목록의 거리 계산 기준이 바뀐다.
     * 동 이름 검색이든 GPS든 같은 형식으로 받는다 (docs/DB.md D14).
     */
    @Transactional
    public LocationResponse changeLocation(Long userId, LocationRequest request) {
        User user = getUser(userId);
        user.changeLocation(request.dongName(), request.latitude(), request.longitude());
        return new LocationResponse(user.getDongName(), user.getLatitude(), user.getLongitude());
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }
}
