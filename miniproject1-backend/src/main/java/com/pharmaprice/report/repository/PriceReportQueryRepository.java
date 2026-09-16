package com.pharmaprice.report.repository;

import com.pharmaprice.report.domain.PriceReport;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PriceReportQueryRepository extends Repository<PriceReport, Long> {

	// reporter는 닉네임만 노출, receipt_file_id는 boolean(hasReceipt)으로만 변환한다.
	@Query(
		value = """
			SELECT r.id AS id,
			       p.id AS pharmacyId, p.name AS pharmacyName,
			       d.id AS drugId, d.display_name AS drugDisplayName, d.package_unit AS drugPackageUnit,
			       r.price AS price, r.purchased_at AS purchasedAt,
			       u.nickname AS reporterNickname,
			       r.source AS source, r.status AS status, r.flagged AS flagged,
			       (r.receipt_file_id IS NOT NULL) AS hasReceipt,
			       r.created_at AS createdAt
			FROM price_report r
			JOIN pharmacy p ON p.id = r.pharmacy_id
			JOIN drug d ON d.id = r.drug_id
			LEFT JOIN app_user u ON u.id = r.user_id
			WHERE r.status = 'ACTIVE'
			  AND (:pharmacyId IS NULL OR r.pharmacy_id = :pharmacyId)
			  AND (:drugId IS NULL OR r.drug_id = :drugId)
			  AND (:userId IS NULL OR r.user_id = :userId)
			ORDER BY r.created_at DESC
			""",
		countQuery = """
			SELECT COUNT(*)
			FROM price_report r
			WHERE r.status = 'ACTIVE'
			  AND (:pharmacyId IS NULL OR r.pharmacy_id = :pharmacyId)
			  AND (:drugId IS NULL OR r.drug_id = :drugId)
			  AND (:userId IS NULL OR r.user_id = :userId)
			""",
		nativeQuery = true)
	Page<PriceReportListProjection> search(
		@Param("pharmacyId") Long pharmacyId, @Param("drugId") Long drugId, @Param("userId") Long userId,
		Pageable pageable);

	interface PriceReportListProjection {
		Long getId();
		Long getPharmacyId();
		String getPharmacyName();
		Long getDrugId();
		String getDrugDisplayName();
		String getDrugPackageUnit();
		Integer getPrice();
		LocalDate getPurchasedAt();
		String getReporterNickname();
		String getSource();
		String getStatus();
		Boolean getFlagged();
		Boolean getHasReceipt();
		Instant getCreatedAt();
	}
}
