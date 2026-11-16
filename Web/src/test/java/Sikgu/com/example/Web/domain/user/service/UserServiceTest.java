package Sikgu.com.example.Web.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import Sikgu.com.example.Web.domain.user.dto.request.LocationRequest;
import Sikgu.com.example.Web.domain.user.dto.response.UserResponses.LocationResponse;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Test
    void 가입_직후_집_위치를_GPS로_설정한다() {
        User user = User.create("user@korea.ac.kr", "$2a$10$hashed", "배고픈 고구마");
        given(userRepository.findById(12L)).willReturn(Optional.of(user));

        userService.changeLocation(12L, new LocationRequest("역삼동", 37.5006, 127.0364));

        assertThat(user.hasLocation()).isTrue();
        assertThat(user.getDongName()).isEqualTo("역삼동");
    }

    @Test
    void 집_위치를_바꾸면_동_이름과_위경도가_모두_바뀐다() {
        User user = User.create("user@korea.ac.kr", "$2a$10$hashed", "배고픈 고구마");
        user.changeLocation("역삼동", 37.5006, 127.0364);
        given(userRepository.findById(12L)).willReturn(Optional.of(user));

        LocationResponse response = userService.changeLocation(12L,
                new LocationRequest("흑석동", 37.5085, 126.9620));

        assertThat(user.getDongName()).isEqualTo("흑석동");
        assertThat(user.getLatitude()).isEqualTo(37.5085);
        assertThat(user.getLongitude()).isEqualTo(126.9620);
        assertThat(response).isEqualTo(new LocationResponse("흑석동", 37.5085, 126.9620));
    }

    @Test
    void 사용자가_없으면_COMMON404() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeLocation(99L, new LocationRequest("흑석동", 37.5, 126.9)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.USER_NOT_FOUND);
    }
}
