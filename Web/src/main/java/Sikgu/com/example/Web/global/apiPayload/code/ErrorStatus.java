package Sikgu.com.example.Web.global.apiPayload.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 목록. 코드 정의는 docs/API.md 1장과 맞춘다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorStatus {

    // 공통
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "요청 값이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "대상을 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 내부 오류가 발생했습니다."),

    // 인증
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH401", "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH403", "권한이 없습니다."),
    NOT_SCHOOL_EMAIL(HttpStatus.BAD_REQUEST, "AUTH4001", "학교 이메일(ac.kr)만 사용할 수 있습니다."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH4002", "인증코드가 일치하지 않습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH4003", "인증코드가 만료되었거나 발송 이력이 없습니다."),
    VERIFICATION_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "AUTH4004", "이메일 인증이 만료되었습니다. 다시 인증해 주세요."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH5001", "인증 메일 발송에 실패했습니다."),

    // 회원
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "USER409", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER409", "이미 가입된 이메일입니다."),
    DUPLICATE_USER(HttpStatus.CONFLICT, "USER409", "이미 사용 중인 아이디 또는 이메일입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
