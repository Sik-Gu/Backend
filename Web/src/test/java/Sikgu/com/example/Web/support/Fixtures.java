package Sikgu.com.example.Web.support;

import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.user.entity.User;
import java.time.LocalDateTime;
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

    /** 역삼동 집 위치 (테스트 기준점) */
    public static final double HOME_LAT = 37.5006;
    public static final double HOME_LNG = 127.0364;

    /** id 없는 새 사용자 (JPA 테스트에서 저장용). 이메일은 name@korea.ac.kr, 집 위치는 역삼동 */
    public static User newUser(String name) {
        User user = User.create(name + "@korea.ac.kr", "$2a$10$hashed", "닉네임 " + name);
        user.changeLocation("역삼동", HOME_LAT, HOME_LNG);
        return user;
    }

    /** 집 위치를 아직 설정하지 않은 사용자 (가입 직후) */
    public static User newUserWithoutLocation(String name) {
        return User.create(name + "@korea.ac.kr", "$2a$10$hashed", "닉네임 " + name);
    }

    /** 생수 6단위, 단위당 4,840원. 호스트의 집 위치가 글에 저장된다 */
    public static Post post(User host, LocalDateTime deadlineAt) {
        return Post.create(host, "코스트코 생수 같이 사요", "정문에서 나눠요", "커클랜드 생수 2L x 24", null,
                Category.FOOD, "2L 생수 4병", 6, 29040, deadlineAt);
    }

    /** id를 넣은 게시글 (단위 테스트용) */
    public static Post post(Long id, User host, LocalDateTime deadlineAt) {
        Post post = post(host, deadlineAt);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private Fixtures() {
    }
}
