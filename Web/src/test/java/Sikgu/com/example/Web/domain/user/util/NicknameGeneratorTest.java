package Sikgu.com.example.Web.domain.user.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.RepeatedTest;

class NicknameGeneratorTest {

    private final NicknameGenerator generator = new NicknameGenerator();

    @RepeatedTest(20)
    void 형용사와_음식을_공백으로_이어_만든다() {
        String nickname = generator.generate();

        assertThat(nickname.split(" ")).hasSize(2);
        assertThat(nickname.length()).isLessThanOrEqualTo(30);
    }
}
