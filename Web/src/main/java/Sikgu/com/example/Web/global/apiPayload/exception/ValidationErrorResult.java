package Sikgu.com.example.Web.global.apiPayload.exception;

import java.util.List;

/**
 * COMMON400 응답의 result. 어떤 필드가 왜 틀렸는지 담는다.
 * <pre>{ "errors": [ { "field": "loginId", "reason": "..." } ] }</pre>
 */
public record ValidationErrorResult(List<FieldErrorDetail> errors) {

    public record FieldErrorDetail(String field, String reason) {
    }
}
