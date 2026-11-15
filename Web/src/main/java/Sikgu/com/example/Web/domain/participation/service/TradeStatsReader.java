package Sikgu.com.example.Web.domain.participation.service;

import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.enums.Category;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자의 거래 통계. 주 거래 카테고리 = 정산완료한 공구가 많은 카테고리 상위 2개.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeStatsReader {

    static final int MAIN_CATEGORY_LIMIT = 2;

    private final ParticipationRepository participationRepository;

    /** 사용자 ID → 주 거래 카테고리 (거래가 없으면 빈 목록) */
    public Map<Long, List<Category>> mainCategories(Collection<Long> userIds) {
        Map<Long, Map<Category, Long>> counts = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Object[] row : participationRepository.countSettledByUserAndCategory(userIds)) {
                counts.computeIfAbsent((Long) row[0], id -> new HashMap<>())
                        .put((Category) row[1], ((Number) row[2]).longValue());
            }
        }

        Map<Long, List<Category>> result = new HashMap<>();
        for (Long userId : userIds) {
            Map<Category, Long> byCategory = counts.getOrDefault(userId, Map.of());
            result.put(userId, byCategory.entrySet().stream()
                    .sorted(Map.Entry.<Category, Long>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder())))
                    .limit(MAIN_CATEGORY_LIMIT)
                    .map(Map.Entry::getKey)
                    .toList());
        }
        return result;
    }
}
