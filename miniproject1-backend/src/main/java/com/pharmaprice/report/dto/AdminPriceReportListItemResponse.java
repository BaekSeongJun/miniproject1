package com.pharmaprice.report.dto;

import com.pharmaprice.report.repository.PriceReportQueryRepository.AdminPriceReportListProjection;
import java.time.Instant;
import java.time.LocalDate;

public record AdminPriceReportListItemResponse(
	Long id, Pharmacy pharmacy, Drug drug, Integer price, LocalDate purchasedAt,
	Reporter reporter, String source, String status, Boolean flagged, String flagReason,
	Long receiptFileId, Instant createdAt
) {
	public record Pharmacy(Long id, String name) {}
	public record Drug(Long id, String displayName, String packageUnit) {}
	public record Reporter(Long id, String nickname, String email) {}

	public static AdminPriceReportListItemResponse from(AdminPriceReportListProjection p) {
		Reporter reporter = p.getReporterId() != null
			? new Reporter(p.getReporterId(), p.getReporterNickname(), p.getReporterEmail())
			: null;
		return new AdminPriceReportListItemResponse(
			p.getId(), new Pharmacy(p.getPharmacyId(), p.getPharmacyName()),
			new Drug(p.getDrugId(), p.getDrugDisplayName(), p.getDrugPackageUnit()),
			p.getPrice(), p.getPurchasedAt(), reporter, p.getSource(), p.getStatus(),
			p.getFlagged(), p.getFlagReason(), p.getReceiptFileId(), p.getCreatedAt());
	}
}
