package Sikgu.com.example.Web.domain.post.repository;

import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.post.enums.PostSort;
import java.time.LocalDateTime;

/**
 * 공구 목록 검색 조건.
 *
 * @param viewerId       조회하는 사람 (차단 관계 글 제외용)
 * @param latitude       조회하는 사람의 동네 위도
 * @param longitude      조회하는 사람의 동네 경도
 * @param keyword        제목·상품명 부분 일치 (없으면 null)
 * @param category       카테고리 (없으면 전체)
 * @param radiusKm       반경 (km)
 * @param recruitingOnly true면 모집중만, false면 취소를 뺀 전체
 */
public record PostSearchCondition(
        Long viewerId,
        double latitude,
        double longitude,
        String keyword,
        Category category,
        int radiusKm,
        boolean recruitingOnly,
        PostSort sort,
        LocalDateTime now,
        int page,
        int size
) {
}
