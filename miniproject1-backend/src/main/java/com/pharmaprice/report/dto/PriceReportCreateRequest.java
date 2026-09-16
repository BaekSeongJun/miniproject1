package com.pharmaprice.report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PriceReportCreateRequest(
	@NotNull Long pharmacyId,
	@NotNull Long drugId,
	@NotNull @Min(100) @Max(200_000) Integer price,
	LocalDate purchasedAt,
	Long receiptFileId,
	@Size(max = 200) String memo
) {
}
