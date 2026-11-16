package com.sikgu.domain.user.nickname;

import com.sikgu.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class NicknameGenerator {

    private static final int MAX_TRY = 10;

    private final UserRepository userRepository;

    public String generateUnique() {
        for (int i = 0; i < MAX_TRY; i++) {
            String nickname = pick(NicknameWords.ADJECTIVES) + pick(NicknameWords.NOUNS);
            if (!userRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }

        while (true) {
            String nickname = pick(NicknameWords.ADJECTIVES) + pick(NicknameWords.NOUNS) + randomFourDigits();
            if (!userRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }
    }

    private String pick(List<String> words) {
        int index = ThreadLocalRandom.current().nextInt(words.size());
        return words.get(index);
    }

    private int randomFourDigits() {
        return ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
