package com.pharmaprice.drug.repository;

import com.pharmaprice.drug.domain.Drug;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface DrugQueryRepository extends Repository<Drug, Long> {

	// T-13: q/category 조건 + pharmacy_drug_price_stat 집계를 조인 한 번으로 처리 (N+1 방지)
	@Query(
		value = """
			SELECT d.id AS id, d.item_seq AS itemSeq, d.display_name AS displayName, d.name AS name,
			       d.maker AS maker, d.category AS category, d.form AS form, d.package_unit AS packageUnit,
			       d.image_url AS imageUrl,
			       ROUND(AVG(s.rep_price))::int AS nationalAvgPrice,
			       COUNT(s.pharmacy_id) AS pharmacyCount
			FROM drug d
			LEFT JOIN pharmacy_drug_price_stat s ON s.drug_id = d.id
			WHERE d.otc_flag = true
			  AND (:q IS NULL OR d.display_name ILIKE '%' || :q || '%' OR d.name ILIKE '%' || :q || '%')
			  AND (:category IS NULL OR d.category = :category)
			GROUP BY d.id
			ORDER BY d.id
			""",
		countQuery = """
			SELECT COUNT(*)
			FROM drug d
			WHERE d.otc_flag = true
			  AND (:q IS NULL OR d.display_name ILIKE '%' || :q || '%' OR d.name ILIKE '%' || :q || '%')
			  AND (:category IS NULL OR d.category = :category)
			""",
		nativeQuery = true)
	Page<DrugSummaryProjection> search(@Param("q") String q, @Param("category") String category, Pageable pageable);

	@Query(
		value = """
			SELECT d.id AS id, d.item_seq AS itemSeq, d.display_name AS displayName, d.name AS name,
			       d.maker AS maker, d.category AS category, d.form AS form, d.package_unit AS packageUnit,
			       d.image_url AS imageUrl,
			       ROUND(AVG(s.rep_price))::int AS nationalAvg,
			       MIN(s.min_price)::int AS nationalMin,
			       MAX(s.max_price)::int AS nationalMax,
			       COUNT(s.pharmacy_id) AS pharmacyCount,
			       COALESCE(SUM(s.report_count), 0) AS reportCount
			FROM drug d
			LEFT JOIN pharmacy_drug_price_stat s ON s.drug_id = d.id
			WHERE d.otc_flag = true AND d.id = :drugId
			GROUP BY d.id
			""",
		nativeQuery = true)
	Optional<DrugDetailProjection> findDetailById(@Param("drugId") long drugId);

	interface DrugSummaryProjection {
		Long getId();
		String getItemSeq();
		String getDisplayName();
		String getName();
		String getMaker();
		String getCategory();
		String getForm();
		String getPackageUnit();
		String getImageUrl();
		Integer getNationalAvgPrice();
		Long getPharmacyCount();
	}

	interface DrugDetailProjection {
		Long getId();
		String getItemSeq();
		String getDisplayName();
		String getName();
		String getMaker();
		String getCategory();
		String getForm();
		String getPackageUnit();
		String getImageUrl();
		Integer getNationalAvg();
		Integer getNationalMin();
		Integer getNationalMax();
		Long getPharmacyCount();
		Long getReportCount();
	}
}
