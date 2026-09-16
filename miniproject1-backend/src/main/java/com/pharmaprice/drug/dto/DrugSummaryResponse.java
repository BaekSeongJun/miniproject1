package com.pharmaprice.drug.dto;

public record DrugSummaryResponse(
	Long id, String itemSeq, String displayName, String name, String maker,
	String category, String form, String packageUnit, String imageUrl,
	Integer nationalAvgPrice, Long pharmacyCount
) {}
