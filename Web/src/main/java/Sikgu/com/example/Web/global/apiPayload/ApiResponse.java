package Sikgu.com.example.Web.global.apiPayload;

import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;

/**
 * 모든 API의 공통 응답 형식.
 * <pre>{ "isSuccess": true, "code": "COMMON200", "message": "성공", "result": { } }</pre>
 */
public record ApiResponse<T>(
        boolean isSuccess,
        String code,
        String message,
        T result
) {

    private static final String SUCCESS_CODE = "COMMON200";
    private static final String SUCCESS_MESSAGE = "성공";

    public static <T> ApiResponse<T> onSuccess(T result) {
        return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, result);
    }

    public static <T> ApiResponse<T> onFailure(ErrorStatus status, T result) {
        return new ApiResponse<>(false, status.getCode(), status.getMessage(), result);
    }
}
