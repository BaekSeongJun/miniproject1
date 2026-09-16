package com.pharmaprice.recommendation.distance;

public final class CoordinateValidator {

    private static final double MIN_LAT = 33.0;
    private static final double MAX_LAT = 39.0;
    private static final double MIN_LNG = 124.0;
    private static final double MAX_LNG = 132.0;

    private CoordinateValidator() {
    }

    public static boolean isValid(double lat, double lng) {
        return lat >= MIN_LAT && lat <= MAX_LAT && lng >= MIN_LNG && lng <= MAX_LNG;
    }

    public static void validate(double lat, double lng) {
        if (!isValid(lat, lng)) {
            throw new IllegalArgumentException("좌표가 대한민국 범위를 벗어났습니다: lat=%s, lng=%s".formatted(lat, lng));
        }
    }
}
