package Sikgu.com.example.Web.domain.image.enums;

/**
 * 이미지 용도. 저장 폴더를 나누는 데 쓴다.
 */
public enum ImageType {
    /** 공구 상품 사진 */
    POST,
    /** 영수증 사진 */
    RECEIPT,
    /** 수령 문의 증거 사진 */
    INQUIRY,
    /** 신고 증빙 사진 */
    REPORT,
    /** 채팅 이미지 (D13) */
    CHAT
}
