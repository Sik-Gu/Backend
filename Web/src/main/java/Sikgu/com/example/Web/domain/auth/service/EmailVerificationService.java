package Sikgu.com.example.Web.domain.auth.service;

import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.EmailSendResponse;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.EmailVerifyResponse;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.global.infra.mail.EmailSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 학교 이메일 인증.
 * <ol>
 *   <li>send: 6자리 코드를 만들어 Redis에 5분 저장하고 메일 발송</li>
 *   <li>verify: 코드가 맞으면 verificationToken(30분)을 발급</li>
 *   <li>signup: 토큰으로 인증된 이메일을 꺼내 쓰고 토큰을 지움</li>
 * </ol>
 * 가입 전까지는 DB에 아무것도 저장하지 않아서, 중간에 이탈해도 빈 계정이 생기지 않는다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    static final Duration CODE_TTL = Duration.ofMinutes(5);
    static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    static final String CODE_KEY_PREFIX = "email:code:";
    static final String TOKEN_KEY_PREFIX = "email:verified:";

    private static final String SCHOOL_EMAIL_SUFFIX = ".ac.kr";

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final SecureRandom random = new SecureRandom();

    public EmailSendResponse sendCode(String rawEmail) {
        String email = normalize(rawEmail);
        if (!email.endsWith(SCHOOL_EMAIL_SUFFIX)) {
            throw new GeneralException(ErrorStatus.NOT_SCHOOL_EMAIL);
        }
        if (userRepository.existsBySchoolEmail(email)) {
            throw new GeneralException(ErrorStatus.DUPLICATE_EMAIL);
        }

        String code = "%06d".formatted(random.nextInt(1_000_000));
        // 다시 보내면 이전 코드를 덮어써서 무효가 된다
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, CODE_TTL);
        emailSender.send(email, "[식구] 이메일 인증코드",
                "인증코드: " + code + "\n" + CODE_TTL.toMinutes() + "분 안에 입력해 주세요.");

        return new EmailSendResponse(email, CODE_TTL.toSeconds());
    }

    public EmailVerifyResponse verifyCode(String rawEmail, String code) {
        String email = normalize(rawEmail);
        String codeKey = CODE_KEY_PREFIX + email;

        String savedCode = redisTemplate.opsForValue().get(codeKey);
        if (savedCode == null) {
            throw new GeneralException(ErrorStatus.VERIFICATION_CODE_EXPIRED);
        }
        if (!constantTimeEquals(savedCode, code)) {
            throw new GeneralException(ErrorStatus.VERIFICATION_CODE_MISMATCH);
        }

        redisTemplate.delete(codeKey);
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, email, TOKEN_TTL);

        return new EmailVerifyResponse(token, TOKEN_TTL.toSeconds());
    }

    /** verificationToken으로 인증된 이메일을 꺼낸다. 없거나 만료면 AUTH4004 */
    public String getVerifiedEmail(String token) {
        String email = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
        if (email == null) {
            throw new GeneralException(ErrorStatus.VERIFICATION_TOKEN_INVALID);
        }
        return email;
    }

    /** 가입이 끝나면 토큰을 지워서 같은 토큰으로 다시 가입하지 못하게 한다. */
    public void consumeToken(String token) {
        redisTemplate.delete(TOKEN_KEY_PREFIX + token);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** 응답 시간 차이로 코드를 추측하지 못하게 고정 시간 비교 */
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
