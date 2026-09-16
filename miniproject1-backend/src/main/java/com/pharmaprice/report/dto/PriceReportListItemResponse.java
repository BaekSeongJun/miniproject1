package com.pharmaprice.report.dto;

import com.pharmaprice.report.repository.PriceReportQueryRepository.PriceReportListProjection;
import java.time.Instant;
import java.time.LocalDate;

public record PriceReportListItemResponse(
	Long id, Pharmacy pharmacy, Drug drug, Integer price, LocalDate purchasedAt,
	Reporter reporter, String source, String status, Boolean flagged, Boolean hasReceipt, Instant createdAt
) {
	public record Pharmacy(Long id, String name) {}
	public record Drug(Long id, String displayName, String packageUnit) {}
	public record Reporter(String nickname) {}

	public static PriceReportListItemResponse from(PriceReportListProjection p) {
		Reporter reporter = p.getReporterNickname() != null ? new Reporter(p.getReporterNickname()) : null;
		return new PriceReportListItemResponse(
			p.getId(), new Pharmacy(p.getPharmacyId(), p.getPharmacyName()),
			new Drug(p.getDrugId(), p.getDrugDisplayName(), p.getDrugPackageUnit()),
			p.getPrice(), p.getPurchasedAt(), reporter, p.getSource(), p.getStatus(),
			p.getFlagged(), p.getHasReceipt(), p.getCreatedAt());
	}
}
