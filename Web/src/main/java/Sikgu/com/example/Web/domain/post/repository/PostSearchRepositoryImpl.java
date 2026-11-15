package Sikgu.com.example.Web.domain.post.repository;

import Sikgu.com.example.Web.domain.post.enums.PostSort;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * 공구 목록 검색 구현.
 *
 * <p>거리는 Haversine 공식으로 DB에서 계산한다. 표준 SQL 함수만 써서 PostgreSQL·H2(테스트) 모두에서 동작한다.
 * 조건은 값이 있을 때만 붙이고, 모든 값은 파라미터로 바인딩한다. ORDER BY는 정해진 문자열 중에서만 고른다.
 */
public class PostSearchRepositoryImpl implements PostSearchRepository {

    private static final String DISTANCE = """
            6371 * ACOS(LEAST(1.0, GREATEST(-1.0,
                COS(RADIANS(:lat)) * COS(RADIANS(p.trade_latitude)) * COS(RADIANS(p.trade_longitude) - RADIANS(:lng))
                + SIN(RADIANS(:lat)) * SIN(RADIANS(p.trade_latitude)))))""";

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<PostSearchRow> search(PostSearchCondition c) {
        Map<String, Object> params = new HashMap<>();
        String inner = innerQuery(c, params);

        Query select = em.createNativeQuery("SELECT t.post_id, t.distance FROM (" + inner + ") t"
                + " WHERE t.distance <= :radius ORDER BY " + orderBy(c) + " LIMIT :limit OFFSET :offset");
        Query count = em.createNativeQuery("SELECT COUNT(*) FROM (" + inner + ") t WHERE t.distance <= :radius");

        params.put("radius", (double) c.radiusKm());
        params.forEach((name, value) -> {
            select.setParameter(name, value);
            count.setParameter(name, value);
        });
        select.setParameter("limit", c.size());
        select.setParameter("offset", (long) c.page() * c.size());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = select.getResultList();
        List<PostSearchRow> content = rows.stream()
                .map(row -> new PostSearchRow(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue()))
                .toList();
        long total = ((Number) count.getSingleResult()).longValue();
        return new PageImpl<>(content, PageRequest.of(c.page(), c.size()), total);
    }

    private static String innerQuery(PostSearchCondition c, Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.post_id, p.created_at, p.deadline_at, p.view_count, p.unit_price, %s AS distance
                FROM posts p
                WHERE 1 = 1""".formatted(DISTANCE));
        params.put("lat", c.latitude());
        params.put("lng", c.longitude());

        if (c.recruitingOnly()) {
            sql.append(" AND p.status = :recruiting");
            params.put("recruiting", PostStatus.RECRUITING.name());
        } else {
            sql.append(" AND p.status <> :cancelled");
            params.put("cancelled", PostStatus.CANCELLED.name());
        }
        if (c.category() != null) {
            sql.append(" AND p.category = :category");
            params.put("category", c.category().name());
        }
        if (c.keyword() != null) {
            sql.append(" AND (LOWER(p.title) LIKE :keyword ESCAPE '!' OR LOWER(p.product_name) LIKE :keyword ESCAPE '!')");
            params.put("keyword", "%" + escapeLike(c.keyword().toLowerCase()) + "%");
        }
        if (c.sort() == PostSort.DEADLINE) {
            // 마감 임박순은 마감이 지나지 않은 글만
            sql.append(" AND p.deadline_at > :now");
            params.put("now", c.now());
        }
        // 차단은 양방향: 내가 차단했거나 나를 차단한 사람의 글은 뺀다
        sql.append(" AND p.host_id NOT IN (SELECT b.blocked_id FROM blocks b WHERE b.blocker_id = :viewerId)")
                .append(" AND p.host_id NOT IN (SELECT b.blocker_id FROM blocks b WHERE b.blocked_id = :viewerId)");
        params.put("viewerId", c.viewerId());
        return sql.toString();
    }

    private static String orderBy(PostSearchCondition c) {
        String latest = "t.created_at DESC, t.post_id DESC";
        return switch (c.sort()) {
            case LATEST -> latest;
            case DEADLINE -> "t.deadline_at ASC, " + latest;
            case POPULAR -> "t.view_count DESC, " + latest;
            case PRICE -> "t.unit_price ASC, " + latest;
        };
    }

    /** LIKE 특수문자(%, _)를 글자 그대로 찾도록 이스케이프 */
    private static String escapeLike(String keyword) {
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
