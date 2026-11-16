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
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH4011", "이메일 또는 비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4012", "다시 로그인해 주세요."),
    LOGGED_OUT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4013", "로그아웃한 토큰입니다. 다시 로그인해 주세요."),
    SUSPENDED_USER(HttpStatus.FORBIDDEN, "AUTH4031", "이용이 정지된 계정입니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH5001", "인증 메일 발송에 실패했습니다."),

    // 회원
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "사용자를 찾을 수 없습니다."),
    LOCATION_REQUIRED(HttpStatus.BAD_REQUEST, "USER4001", "집 위치를 먼저 설정해 주세요."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER409", "이미 가입된 이메일입니다."),

    // 이미지
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "IMAGE4001", "jpg, png, webp 이미지만 올릴 수 있습니다."),
    INVALID_IMAGE_COUNT(HttpStatus.BAD_REQUEST, "IMAGE4002", "이미지는 1~10개까지 올릴 수 있습니다."),
    IMAGE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "IMAGE4131", "이미지는 한 장에 10MB까지 올릴 수 있습니다."),
    IMAGE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE5001", "이미지 저장에 실패했습니다."),

    // 공구 게시글
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "공구를 찾을 수 없습니다."),
    NOT_POST_HOST(HttpStatus.FORBIDDEN, "AUTH403", "공구를 연 사람만 할 수 있습니다."),
    INVALID_POST_STATUS(HttpStatus.CONFLICT, "POST409", "지금 공구 상태에서는 할 수 없는 요청입니다."),
    INVALID_HOST_UNITS(HttpStatus.BAD_REQUEST, "POST4001", "호스트 구매 단위는 1 이상, 총 단위보다 작아야 합니다."),
    INDIVISIBLE_PRICE(HttpStatus.BAD_REQUEST, "POST4003", "총 가격이 총 단위 수로 나누어떨어져야 합니다."),
    POST_NOT_EDITABLE(HttpStatus.CONFLICT, "POST4091", "모집중이고 다른 참여자가 없을 때만 수정할 수 있습니다."),
    POST_NOT_CANCELLABLE(HttpStatus.CONFLICT, "POST4092", "주문완료 이후에는 공구를 취소할 수 없습니다."),

    INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "SEARCH4001", "완성된 단어 또는 초성만 검색할 수 있습니다."),

    // 참여
    NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "AUTH403", "이 공구의 참여자가 아닙니다."),
    EXCEED_REMAINING_UNITS(HttpStatus.BAD_REQUEST, "PART4001", "남은 단위보다 많이 신청할 수 없습니다."),
    BLOCKED_HOST(HttpStatus.FORBIDDEN, "PART4031", "차단 관계인 사용자의 공구에는 참여할 수 없습니다."),
    ALREADY_PARTICIPATED(HttpStatus.CONFLICT, "PART4091", "이미 참여한 공구입니다."),
    HOST_CANNOT_LEAVE(HttpStatus.CONFLICT, "PART4092", "호스트는 참여를 취소할 수 없습니다. 공구 취소를 이용해 주세요."),
    ALREADY_RECEIVED(HttpStatus.CONFLICT, "PART4093", "이미 받았어요를 눌렀습니다."),

    // 나눔 장소 합의
    DISTRIBUTION_PLACE_NOT_PROPOSED(HttpStatus.CONFLICT, "PLACE4091", "호스트가 아직 나눔 장소를 제안하지 않았습니다."),
    DISTRIBUTION_PLACE_NOT_AGREED(HttpStatus.CONFLICT, "PLACE4092", "참여자 전원이 나눔 장소에 동의해야 주문할 수 있습니다."),
    PLACE_ALREADY_AGREED(HttpStatus.CONFLICT, "PLACE4093", "이미 나눔 장소에 동의했습니다."),

    // 수령 문의
    INVALID_RECEIVED_UNITS(HttpStatus.BAD_REQUEST, "INQ4001", "수량 부족 문의는 실제로 받은 단위(0 이상, 내 단위 미만)가 필요합니다."),
    INQUIRY_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "INQ4002", "다른 상품·파손 문의는 사진이 1장 이상 필요합니다."),
    INQUIRY_AFTER_RECEIVED(HttpStatus.CONFLICT, "INQ4091", "이미 받았어요를 눌러 문의할 수 없습니다."),
    INQUIRY_IN_PROGRESS(HttpStatus.CONFLICT, "INQ4092", "수령 문의를 운영팀이 처리하고 있습니다."),

    // 주문
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "주문 정보가 아직 없습니다."),
    ORDER_ALREADY_EXISTS(HttpStatus.CONFLICT, "ORDER4091", "이미 주문 정보가 등록됐습니다."),

    // 후기
    REVIEW_TARGET_INVALID(HttpStatus.BAD_REQUEST, "REVIEW4001", "평가할 수 없는 대상입니다."),
    REVIEW_DUPLICATED(HttpStatus.CONFLICT, "REVIEW4091", "이미 후기를 작성한 대상입니다."),
    REVIEW_PERIOD_CLOSED(HttpStatus.CONFLICT, "REVIEW4092", "후기는 정산완료 후 3일 안에만 쓸 수 있습니다."),

    // 신고
    CANNOT_REPORT_SELF(HttpStatus.BAD_REQUEST, "REPORT4001", "자기 자신은 신고할 수 없습니다."),
    REPORT_DETAIL_REQUIRED(HttpStatus.BAD_REQUEST, "REPORT4002",
            "신고 사유에 필요한 값이 없습니다. (채팅: 메시지, 거래 위반: 위반 단계, 기타: 내용)"),

    // 차단
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "BLOCK4001", "자기 자신은 차단할 수 없습니다."),
    ALREADY_BLOCKED(HttpStatus.CONFLICT, "BLOCK4091", "이미 차단한 사용자입니다."),
    BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "차단한 적이 없는 사용자입니다."),

    // 포인트 (포인트 담당 구현에서 사용. integration.point.PointClient 참고)
    INSUFFICIENT_POINT(HttpStatus.CONFLICT, "POINT409", "포인트 잔액이 부족합니다."),
    UNPAID_DEBT(HttpStatus.FORBIDDEN, "POINT4031", "미납 미수금이 있어 공구에 참여하거나 공구를 열 수 없습니다."),
    POINT_CONFLICT(HttpStatus.CONFLICT, "POINT4091", "잠시 후 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
