package Sikgu.com.example.Web.domain.post.repository;

import org.springframework.data.domain.Page;

/**
 * 공구 목록 검색. 거리 계산(반경 필터·거리 표시)이 필요해서 SQL을 직접 만든다.
 */
public interface PostSearchRepository {

    /** 조건에 맞는 게시글 ID와 거리를 정렬·페이지 순서대로 돌려준다 */
    Page<PostSearchRow> search(PostSearchCondition condition);

    /**
     * @param distanceMeters 조회하는 사람의 집 ↔ 글 올린 사람의 집(글에 저장된 위치) 거리. 응답에는 구간으로만 쓴다
     */
    record PostSearchRow(Long postId, double distanceMeters) {
    }
}
