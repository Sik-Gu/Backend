package com.sikgu.web.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청이에요"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "요청한 대상을 찾을 수 없어요"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 오류가 발생했어요"),

    // 인증
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH401", "로그인이 필요해요"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH403", "권한이 없어요"),

    // 사용자
    LOGIN_ID_DUPLICATED(HttpStatus.CONFLICT, "USER409", "이미 사용 중인 아이디예요");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}