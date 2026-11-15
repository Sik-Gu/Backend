package Sikgu.com.example.Web.domain.participation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import Sikgu.com.example.Web.domain.participation.repository.ParticipationRepository;
import Sikgu.com.example.Web.domain.post.enums.Category;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeStatsReaderTest {

    @Mock
    ParticipationRepository participationRepository;

    @InjectMocks
    TradeStatsReader reader;

    @Test
    void 거래가_많은_카테고리_상위_2개를_고르고_거래가_없으면_빈_목록() {
        given(participationRepository.countSettledByUserAndCategory(List.of(1L, 2L))).willReturn(List.of(
                new Object[] {1L, Category.FOOD, 5L},
                new Object[] {1L, Category.DAILY, 3L},
                new Object[] {1L, Category.ETC, 1L}));

        Map<Long, List<Category>> result = reader.mainCategories(List.of(1L, 2L));

        assertThat(result.get(1L)).containsExactly(Category.FOOD, Category.DAILY);
        assertThat(result.get(2L)).isEmpty();
    }
}
