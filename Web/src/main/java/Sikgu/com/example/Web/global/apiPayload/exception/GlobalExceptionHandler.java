package Sikgu.com.example.Web.global.apiPayload.exception;

import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 서비스에서 던진 비즈니스 예외 */
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(GeneralException e) {
        ErrorStatus status = e.getErrorStatus();
        return ResponseEntity.status(status.getHttpStatus())
                .body(ApiResponse.onFailure(status, null));
    }

    /** @RequestBody DTO 검증 실패 (@Valid) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ValidationErrorResult>> handleBodyValidation(MethodArgumentNotValidException e) {
        List<ValidationErrorResult.FieldErrorDetail> errors = e.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toDetail)
                .toList();
        return badRequest(errors);
    }

    /** @RequestParam 등 메서드 파라미터 검증 실패 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<ValidationErrorResult>> handleParamValidation(HandlerMethodValidationException e) {
        List<ValidationErrorResult.FieldErrorDetail> errors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ValidationErrorResult.FieldErrorDetail(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return badRequest(errors);
    }

    /** 필수 쿼리 파라미터 누락 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<ValidationErrorResult>> handleMissingParam(MissingServletRequestParameterException e) {
        return badRequest(List.of(new ValidationErrorResult.FieldErrorDetail(e.getParameterName(), "필수 값입니다.")));
    }

    /** 쿼리 파라미터 타입 오류 (예: 없는 ENUM 값) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ValidationErrorResult>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return badRequest(List.of(new ValidationErrorResult.FieldErrorDetail(e.getName(), "올바르지 않은 값입니다.")));
    }

    /** 업로드 용량 초과 (spring.servlet.multipart 설정) */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(ErrorStatus.IMAGE_TOO_LARGE.getHttpStatus())
                .body(ApiResponse.onFailure(ErrorStatus.IMAGE_TOO_LARGE, null));
    }

    /** JSON 형식 오류, 없는 ENUM 값 등 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorStatus.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST, null));
    }

    /** 처리하지 못한 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("처리하지 못한 예외", e);
        return ResponseEntity.status(ErrorStatus.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR, null));
    }

    private static ValidationErrorResult.FieldErrorDetail toDetail(FieldError error) {
        return new ValidationErrorResult.FieldErrorDetail(error.getField(), error.getDefaultMessage());
    }

    private static ResponseEntity<ApiResponse<ValidationErrorResult>> badRequest(
            List<ValidationErrorResult.FieldErrorDetail> errors) {
        return ResponseEntity.status(ErrorStatus.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST, new ValidationErrorResult(errors)));
    }
}
