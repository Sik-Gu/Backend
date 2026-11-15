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
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON405", "지원하지 않는 요청 방식이에요"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 오류가 발생했어요"),

    // 인증
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH401", "로그인이 필요해요"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH403", "권한이 없어요"),

    // 사용자
    LOGIN_ID_DUPLICATED(HttpStatus.CONFLICT, "USER409", "이미 사용 중인 아이디예요"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "USER401", "아이디 또는 비밀번호가 일치하지 않아요"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404", "사용자를 찾을 수 없어요"),

    // 게시글
    PRICE_NOT_DIVISIBLE(HttpStatus.BAD_REQUEST, "POST400", "총 가격이 단위 수로 나누어떨어지지 않아요"),
    INVALID_HOST_UNITS(HttpStatus.BAD_REQUEST, "POST400", "호스트 구매 수량은 총 단위 수보다 적어야 해요"),
    NOT_POST_HOST(HttpStatus.FORBIDDEN, "POST403", "호스트만 할 수 있어요"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST404", "공구를 찾을 수 없어요"),
    POST_NOT_RECRUITING(HttpStatus.CONFLICT, "POST409", "모집중인 공구만 수정·취소할 수 있어요");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}