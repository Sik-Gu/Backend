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

    /** id 없는 새 사용자 (JPA 테스트에서 저장용) */
    public static User newUser(String loginId) {
        return User.create(loginId, loginId + "@korea.ac.kr", "$2a$10$hashed", "닉네임 " + loginId,
                "역삼동", 37.5006, 127.0364);
    }

    /** 생수 6단위, 단위당 4,840원, 역삼동 거래 장소 */
    public static Post post(User host, LocalDateTime deadlineAt) {
        return Post.create(host, "코스트코 생수 같이 사요", "정문에서 나눠요", "커클랜드 생수 2L x 24", null,
                Category.FOOD, "2L 생수 4병", 6, 29040, deadlineAt, "정문 앞", "역삼동", 37.5012, 127.0396);
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
