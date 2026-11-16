package Sikgu.com.example.Web.domain.post.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.support.Fixtures;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PostTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);

    private final Post post = Fixtures.post(1L, Fixtures.user(10L), NOW.plusDays(2));

    @Test
    void 단위당_가격은_총가격을_총단위로_나눈_값이다() {
        assertThat(post.getUnitPrice()).isEqualTo(29040 / 6);
        assertThat(post.getStatus()).isEqualTo(PostStatus.RECRUITING);
    }

    @Test
    void 참여하면_단위와_인원이_늘고_다_차면_true() {
        assertThat(post.addParticipant(2)).isFalse();
        assertThat(post.addParticipant(4)).isTrue();
        assertThat(post.getFilledUnits()).isEqualTo(6);
        assertThat(post.getParticipantCount()).isEqualTo(2);
    }

    @Test
    void 남은_단위보다_많이_신청하면_PART4001() {
        post.addParticipant(5);

        assertThatThrownBy(() -> post.addParticipant(2))
                .extracting("errorStatus").isEqualTo(ErrorStatus.EXCEED_REMAINING_UNITS);
        assertThat(post.getFilledUnits()).isEqualTo(5);
    }

    @Test
    void 모집완료에서_참여_취소하면_모집중으로_돌아간다() {
        post.addParticipant(6);
        post.markRecruited(NOW);

        post.removeParticipant(1);

        assertThat(post.getStatus()).isEqualTo(PostStatus.RECRUITING);
        assertThat(post.getRecruitedAt()).isNull();
        assertThat(post.getFilledUnits()).isEqualTo(5);
    }

    @Test
    void 상태는_정해진_순서로만_바뀌고_시각을_남긴다() {
        post.markRecruited(NOW);
        post.markOrdered(NOW.plusHours(1));
        post.markShipping(NOW.plusHours(2));
        post.markDistributed(NOW.plusHours(3));
        post.markSettled(NOW.plusHours(4));

        assertThat(post.getStatus()).isEqualTo(PostStatus.SETTLED);
        assertThat(post.getOrderedAt()).isEqualTo(NOW.plusHours(1));
        assertThat(post.getSettledAt()).isEqualTo(NOW.plusHours(4));
    }

    @Test
    void 순서를_건너뛰면_POST409() {
        assertThatThrownBy(() -> post.markOrdered(NOW))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_POST_STATUS);
    }

    @Test
    void 주문완료_이후에는_취소할_수_없다() {
        post.markRecruited(NOW);
        post.markOrdered(NOW);

        assertThatThrownBy(() -> post.cancel(NOW))
                .extracting("errorStatus").isEqualTo(ErrorStatus.POST_NOT_CANCELLABLE);
    }

    @Test
    void 모집완료_상태에서는_취소할_수_있다() {
        post.markRecruited(NOW);

        post.cancel(NOW);

        assertThat(post.getStatus()).isEqualTo(PostStatus.CANCELLED);
        assertThat(post.getCancelledAt()).isEqualTo(NOW);
    }
}
