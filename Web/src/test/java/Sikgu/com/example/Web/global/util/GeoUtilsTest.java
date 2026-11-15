package Sikgu.com.example.Web.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Test
    void 두_지점의_거리를_소수_첫째_자리까지_계산한다() {
        // 역삼동 ↔ 흑석동, 약 9km
        Double km = GeoUtils.distanceKm(37.5006, 127.0364, 37.5085, 126.9620);

        assertThat(km).isCloseTo(6.6, Offset.offset(0.5));
        assertThat(km * 10).isEqualTo(Math.rint(km * 10));
    }

    @Test
    void 같은_지점이면_0() {
        assertThat(GeoUtils.distanceKm(37.5, 127.0, 37.5, 127.0)).isZero();
    }

    @Test
    void 좌표가_없으면_null() {
        assertThat(GeoUtils.distanceKm(null, 127.0, 37.5, 127.0)).isNull();
    }
}
