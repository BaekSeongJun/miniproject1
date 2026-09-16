package com.pharmaprice.pharmacy.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PharmacyDetailResponse(
	Long id, String name, String addressRoad, String addressJibun, Double lat, Double lng, String phone,
	Map<String, List<String>> businessHours, Long distanceM, PharmacySummaryResponse.Region region,
	List<DrugPrice> drugPrices
) {
	public record DrugPrice(
		Long drugId, String displayName, String packageUnit,
		Integer repPrice, Integer minPrice, Integer maxPrice, Integer avgPrice,
		Integer reportCount, LocalDate lastReportedAt,
		Integer nationalAvgPrice, Integer diffFromNationalAvg
	) {}
}
