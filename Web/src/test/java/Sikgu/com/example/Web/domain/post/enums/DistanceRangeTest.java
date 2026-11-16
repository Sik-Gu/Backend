package Sikgu.com.example.Web.domain.post.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DistanceRangeTest {

    @Test
    void 거리를_500m_1km_구간으로만_나눈다() {
        assertThat(DistanceRange.of(0.0)).isEqualTo(DistanceRange.WITHIN_500M);
        assertThat(DistanceRange.of(500.0)).isEqualTo(DistanceRange.WITHIN_500M);
        assertThat(DistanceRange.of(500.1)).isEqualTo(DistanceRange.WITHIN_1KM);
        assertThat(DistanceRange.of(1000.0)).isEqualTo(DistanceRange.WITHIN_1KM);
        assertThat(DistanceRange.of(1000.1)).isEqualTo(DistanceRange.OVER_1KM);
        assertThat(DistanceRange.of(null)).isNull();
    }
}
