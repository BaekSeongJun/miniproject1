package com.pharmaprice.report.dto;

import com.pharmaprice.report.domain.FlagReason;
import com.pharmaprice.report.domain.PriceReport;
import com.pharmaprice.report.domain.ReportStatus;
import java.time.Instant;
import java.time.LocalDate;

public record PriceReportResponse(
	Long id,
	Long pharmacyId,
	Long drugId,
	Integer price,
	LocalDate purchasedAt,
	ReportStatus status,
	Boolean flagged,
	FlagReason flagReason,
	Instant createdAt,
	String warning,
	UpdatedStat updatedStat
) {
	public static PriceReportResponse of(PriceReport report, String warning, UpdatedStat updatedStat) {
		return new PriceReportResponse(
			report.getId(), report.getPharmacy().getId(), report.getDrug().getId(),
			report.getPrice(), report.getPurchasedAt(), report.getStatus(), report.getFlagged(),
			report.getFlagReason(), report.getCreatedAt(), warning, updatedStat);
	}

	public record UpdatedStat(
		Integer repPrice, Integer minPrice, Integer avgPrice, Integer reportCount, LocalDate lastReportedAt
	) {
	}
}
