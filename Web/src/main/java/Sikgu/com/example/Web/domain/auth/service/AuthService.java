package Sikgu.com.example.Web.domain.auth.service;

import Sikgu.com.example.Web.domain.auth.dto.request.SignupRequest;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.CheckIdResponse;
import Sikgu.com.example.Web.domain.auth.dto.response.AuthResponses.SignupResponse;
import Sikgu.com.example.Web.domain.point.entity.PointWallet;
import Sikgu.com.example.Web.domain.point.repository.PointWalletRepository;
import Sikgu.com.example.Web.domain.user.dto.request.LocationRequest;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.domain.user.util.NicknameGenerator;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PointWalletRepository pointWalletRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final NicknameGenerator nicknameGenerator;

    public CheckIdResponse checkLoginId(String loginId) {
        return new CheckIdResponse(loginId, !userRepository.existsByLoginId(loginId));
    }

    /**
     * 회원가입. 사용자와 포인트 지갑(잔액 0)을 한 트랜잭션으로 만든다.
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = emailVerificationService.getVerifiedEmail(request.verificationToken());

        if (userRepository.existsByLoginId(request.loginId())) {
            throw new GeneralException(ErrorStatus.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsBySchoolEmail(email)) {
            throw new GeneralException(ErrorStatus.DUPLICATE_EMAIL);
        }

        LocationRequest location = request.location();
        User user = User.create(
                request.loginId(),
                email,
                passwordEncoder.encode(request.password()),
                nicknameGenerator.generate(),
                location.dongName(),
                location.latitude(),
                location.longitude());

        try {
            // 위의 중복 확인과 저장 사이에 같은 아이디로 동시에 가입하는 경우를 UNIQUE 제약으로 막는다
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(ErrorStatus.DUPLICATE_USER);
        }
        pointWalletRepository.save(PointWallet.createFor(user));

        emailVerificationService.consumeToken(request.verificationToken());
        return new SignupResponse(user.getId(), user.getNickname());
    }
}
