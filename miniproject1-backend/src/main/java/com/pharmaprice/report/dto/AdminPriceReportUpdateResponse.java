package com.pharmaprice.report.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pharmaprice.report.domain.PriceReport;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminPriceReportUpdateResponse(
	Long id, String status, Boolean flagged, Instant updatedAt, RecalculatedStat recalculatedStat
) {
	public record RecalculatedStat(Long pharmacyId, Long drugId, Integer repPrice, Integer reportCount) {}

	public static AdminPriceReportUpdateResponse of(PriceReport report, RecalculatedStat recalculatedStat) {
		return new AdminPriceReportUpdateResponse(
			report.getId(), report.getStatus().name(), report.getFlagged(), report.getUpdatedAt(), recalculatedStat);
	}
}
