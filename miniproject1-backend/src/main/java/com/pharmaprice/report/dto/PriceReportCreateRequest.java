package com.pharmaprice.report.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// drugId 또는 newDrug 중 정확히 하나만 있어야 한다 (PriceReportService.create()에서 검증).
public record PriceReportCreateRequest(
	@NotNull Long pharmacyId,
	Long drugId,
	@Valid NewDrugRequest newDrug,
	@NotNull @Min(100) @Max(200_000) Integer price,
	LocalDate purchasedAt,
	Long receiptFileId,
	@Size(max = 200) String memo
) {
	public record NewDrugRequest(
		@NotBlank @Size(max = 100) String displayName,
		@NotBlank String category
	) {
	}
}
