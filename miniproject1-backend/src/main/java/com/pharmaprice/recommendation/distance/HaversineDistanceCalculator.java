package com.pharmaprice.recommendation.distance;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class HaversineDistanceCalculator implements DistanceCalculator {

    private static final double EARTH_RADIUS_M = 6_371_000;
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;
    private static final Set<Integer> ALLOWED_RADIUS_M = Set.of(500, 1000, 2000, 5000);

    @Override
    public BoundingBox boundingBox(double lat, double lng, int radiusM) {
        if (!ALLOWED_RADIUS_M.contains(radiusM)) {
            throw new IllegalArgumentException("허용되지 않는 반경입니다: " + radiusM);
        }
        double latDelta = radiusM / METERS_PER_DEGREE_LAT;
        double lngDelta = radiusM / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));
        return new BoundingBox(lat - latDelta, lat + latDelta, lng - lngDelta, lng + lngDelta);
    }

    @Override
    public double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return EARTH_RADIUS_M * 2 * Math.asin(Math.sqrt(a));
    }
}
