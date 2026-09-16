package com.pharmaprice.pharmacy.dto;

public record PharmacySummaryResponse(
	Long id, String name, String addressRoad, Double lat, Double lng, String phone,
	Long distanceM, Region region
) {
	public record Region(String code, String sido, String sigungu) {}
}
