package Sikgu.com.example.Web.support;

import Sikgu.com.example.Web.domain.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 테스트용 객체 생성 도우미.
 */
public final class Fixtures {

    /** 역삼동 사용자 (DB에 저장하지 않는 단위 테스트용으로 id를 직접 넣는다) */
    public static User user(Long id) {
        User user = newUser("user" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /** id 없는 새 사용자 (JPA 테스트에서 저장용) */
    public static User newUser(String loginId) {
        return User.create(loginId, loginId + "@korea.ac.kr", "$2a$10$hashed", "닉네임 " + loginId,
                "역삼동", 37.5006, 127.0364);
    }

    private Fixtures() {
    }
}
