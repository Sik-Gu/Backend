package Sikgu.com.example.Web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 스프링 컨텍스트 전체가 뜨는지 확인한다.
 * 테스트에서는 H2 메모리 DB를 쓰고, JWT 키는 .env가 없어도 되도록 테스트용 값을 넣는다.
 */
@SpringBootTest(properties = "jwt.secret=test-secret-key-for-sikgu-jwt-must-be-32-bytes-or-more")
class WebApplicationTests {

	@Test
	void contextLoads() {
	}

}
