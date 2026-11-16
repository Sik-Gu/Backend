package Sikgu.com.example.Web.global.apiPayload.exception;

import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 서비스에서 던지는 예외.
 * GlobalExceptionHandler가 공통 응답 형식으로 바꿔준다.
 */
@Getter
public class GeneralException extends RuntimeException {

    private final ErrorStatus errorStatus;

    public GeneralException(ErrorStatus errorStatus) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
    }
}
