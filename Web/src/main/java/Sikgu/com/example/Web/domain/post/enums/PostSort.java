package Sikgu.com.example.Web.domain.post.enums;

/** 공구 목록 정렬. 같은 순위면 최신 등록순 */
public enum PostSort {
    /** 최신 등록순 */
    LATEST,
    /** 마감 임박순 (마감이 지나지 않은 글만) */
    DEADLINE,
    /** 조회수 많은 순 */
    POPULAR,
    /** 단위당 가격 낮은 순 */
    PRICE
}
