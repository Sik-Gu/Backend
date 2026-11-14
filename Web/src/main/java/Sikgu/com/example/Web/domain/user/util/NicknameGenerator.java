package Sikgu.com.example.Web.domain.user.util;

import java.security.SecureRandom;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * "형용사 + 음식" 형식의 랜덤 닉네임을 만든다. (예: 배고픈 고구마)
 * 닉네임은 중복을 허용한다.
 */
@Component
public class NicknameGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "배고픈", "졸린", "신난", "수줍은", "용감한", "느긋한", "부지런한", "행복한", "씩씩한", "상큼한");

    private static final List<String> FOODS = List.of(
            "고구마", "감자", "두부", "당근", "양파", "브로콜리", "버섯", "계란", "만두", "김밥");

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String food = FOODS.get(random.nextInt(FOODS.size()));
        return adjective + " " + food;
    }
}
