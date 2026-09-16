package com.pharmaprice.recommendation.distance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class HaversineDistanceCalculatorTest {

    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    void 강남역_역삼역_거리는_실제값_대비_오차_1퍼센트_이내다() {
        double distance = calculator.distanceMeters(37.4979, 127.0276, 37.5006, 127.0366);

        assertThat(distance).isCloseTo(850, org.assertj.core.data.Percentage.withPercentage(1));
    }

    @Test
    void 바운딩_박스는_반경_원의_사방_경계를_포함한다() {
        double lat = 37.5;
        double lng = 127.0;
        int radiusM = 1000;
        BoundingBox box = calculator.boundingBox(lat, lng, radiusM);

        assertThat(calculator.distanceMeters(lat, lng, box.maxLat(), lng)).isCloseTo(radiusM, org.assertj.core.data.Percentage.withPercentage(1));
        assertThat(calculator.distanceMeters(lat, lng, box.minLat(), lng)).isCloseTo(radiusM, org.assertj.core.data.Percentage.withPercentage(1));
        assertThat(calculator.distanceMeters(lat, lng, lat, box.maxLng())).isCloseTo(radiusM, org.assertj.core.data.Percentage.withPercentage(1));
        assertThat(calculator.distanceMeters(lat, lng, lat, box.minLng())).isCloseTo(radiusM, org.assertj.core.data.Percentage.withPercentage(1));
    }

    @Test
    void 위도가_다르면_경도_델타가_cos_보정으로_달라진다() {
        BoundingBox low = calculator.boundingBox(33.0, 127.0, 1000);
        BoundingBox high = calculator.boundingBox(38.0, 127.0, 1000);

        double lngDeltaLow = low.maxLng() - low.minLng();
        double lngDeltaHigh = high.maxLng() - high.minLng();

        assertThat(lngDeltaHigh).isGreaterThan(lngDeltaLow);
    }

    @Test
    void 허용되지_않는_반경은_예외를_던진다() {
        assertThatThrownBy(() -> calculator.boundingBox(37.5, 127.0, 1500))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
