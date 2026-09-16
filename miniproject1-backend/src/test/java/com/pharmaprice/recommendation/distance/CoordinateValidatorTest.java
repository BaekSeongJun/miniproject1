package com.pharmaprice.recommendation.distance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CoordinateValidatorTest {

    @Test
    void 대한민국_범위_안의_좌표는_유효하다() {
        assertThat(CoordinateValidator.isValid(37.5, 127.0)).isTrue();
    }

    @Test
    void 범위_밖의_좌표는_예외를_던진다() {
        assertThatThrownBy(() -> CoordinateValidator.validate(10.0, 127.0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
