package com.pharmaprice.recommendation.dto;

public record ScoreBreakdown(
	double priceScore,
	double distanceScore,
	double freshnessScore,
	Weights weights
) {
	public record Weights(double price, double distance, double freshness) {}
}
