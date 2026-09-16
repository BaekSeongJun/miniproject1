package com.pharmaprice.recommendation.distance;

public interface DistanceCalculator {

    BoundingBox boundingBox(double lat, double lng, int radiusM);

    double distanceMeters(double lat1, double lng1, double lat2, double lng2);
}
