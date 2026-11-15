package com.sikgu.web.domain.user.service;

import com.sikgu.web.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class NicknameGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "배고픈", "졸린", "용감한", "수줍은", "행복한", "느긋한", "부지런한", "신난");
    private static final List<String> NOUNS = List.of(
            "고구마", "감자", "브로콜리", "당근", "두부", "양파", "버섯", "옥수수");
    private static final int MAX_TRY = 10;

    private final UserRepository userRepository;

    public String generate() {
        for (int i = 0; i < MAX_TRY; i++) {
            String nickname = pick(ADJECTIVES) + " " + pick(NOUNS);
            if (!userRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }

        String nickname;
        do {
            nickname = pick(ADJECTIVES) + " " + pick(NOUNS) + " "
                    + ThreadLocalRandom.current().nextInt(1000, 10000);
        } while (userRepository.existsByNickname(nickname));
        return nickname;
    }

    private String pick(List<String> words) {
        return words.get(ThreadLocalRandom.current().nextInt(words.size()));
    }
}