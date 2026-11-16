package Sikgu.com.example.Web.domain.post.repository;

import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.post.enums.PostSort;
import Sikgu.com.example.Web.domain.post.search.PostSearchKeyword;
import java.time.LocalDateTime;

/**
 * 공구 목록 검색 조건.
 *
 * @param viewerId       조회하는 사람 (차단 관계 글 제외용)
 * @param latitude       조회하는 사람의 집 위도
 * @param longitude      조회하는 사람의 집 경도
 * @param keyword        해석한 검색어: 정확한 단어 또는 초성 (없으면 null)
 * @param category       카테고리 (없으면 전체)
 * @param radiusMeters   반경 (m). 글에 저장된 호스트 집 위치까지의 거리로 거른다
 * @param recruitingOnly true면 모집중만, false면 취소를 뺀 전체
 */
public record PostSearchCondition(
        Long viewerId,
        double latitude,
        double longitude,
        PostSearchKeyword.Parsed keyword,
        Category category,
        int radiusMeters,
        boolean recruitingOnly,
        PostSort sort,
        LocalDateTime now,
        int page,
        int size
) {
}
