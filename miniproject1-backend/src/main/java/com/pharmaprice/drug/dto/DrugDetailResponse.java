package com.pharmaprice.drug.dto;

public record DrugDetailResponse(
	Long id, String itemSeq, String displayName, String name, String maker,
	String category, String form, String packageUnit, String imageUrl,
	PriceStats priceStats
) {
	public record PriceStats(
		Integer nationalAvg, Integer nationalMin, Integer nationalMax,
		Long pharmacyCount, Long reportCount
	) {}
}
