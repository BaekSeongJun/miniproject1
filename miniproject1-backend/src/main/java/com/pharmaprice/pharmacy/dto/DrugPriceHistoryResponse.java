package com.pharmaprice.pharmacy.dto;

import java.time.LocalDate;
import java.util.List;

public record DrugPriceHistoryResponse(
	Long pharmacyId, Long drugId, List<Point> points
) {
	public record Point(LocalDate purchasedAt, Integer price, Boolean flagged) {}
}
