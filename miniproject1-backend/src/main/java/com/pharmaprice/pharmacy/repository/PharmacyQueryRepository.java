package com.pharmaprice.pharmacy.repository;

import com.pharmaprice.pharmacy.domain.Pharmacy;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PharmacyQueryRepository extends Repository<Pharmacy, Long> {

	// T-19: q/좌표 조건 + region 조인을 한 번에 처리 (N+1 방지). 반경 필터는 바운딩 박스로 1차 축소 후 서비스에서 정밀 거리로 재필터링한다.
	@Query(
		value = """
			SELECT p.id AS id, p.name AS name, p.address_road AS addressRoad,
			       p.lat AS lat, p.lng AS lng, p.phone AS phone,
			       r.code AS regionCode, r.sido AS regionSido, r.sigungu AS regionSigungu
			FROM pharmacy p
			LEFT JOIN region r ON r.code = p.region_code
			WHERE p.is_active = true
			  AND (:q IS NULL OR p.name ILIKE '%' || :q || '%' OR p.address_road ILIKE '%' || :q || '%')
			  AND (:minLat IS NULL OR p.lat BETWEEN :minLat AND :maxLat)
			  AND (:minLng IS NULL OR p.lng BETWEEN :minLng AND :maxLng)
			ORDER BY p.id
			""",
		countQuery = """
			SELECT COUNT(*)
			FROM pharmacy p
			WHERE p.is_active = true
			  AND (:q IS NULL OR p.name ILIKE '%' || :q || '%' OR p.address_road ILIKE '%' || :q || '%')
			  AND (:minLat IS NULL OR p.lat BETWEEN :minLat AND :maxLat)
			  AND (:minLng IS NULL OR p.lng BETWEEN :minLng AND :maxLng)
			""",
		nativeQuery = true)
	Page<PharmacySummaryProjection> search(
		@Param("q") String q,
		@Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
		@Param("minLng") Double minLng, @Param("maxLng") Double maxLng,
		Pageable pageable);

	// JSONB(businessHours)는 네이티브 프로젝션으로 매핑이 까다로워 엔티티 fetch join으로 조회한다. 단일 건이라 N+1 우려 없음.
	@Query("""
		SELECT p FROM Pharmacy p LEFT JOIN FETCH p.region
		WHERE p.isActive = true AND p.id = :pharmacyId
		""")
	Optional<Pharmacy> findActiveDetailById(@Param("pharmacyId") long pharmacyId);

	// 해당 약국이 취급하는 약품별 대표가/전국 평균가. 전국 평균은 같은 약품의 전체 약국 통계를 서브쿼리로 집계한다.
	@Query(
		value = """
			SELECT s.drug_id AS drugId, d.display_name AS displayName, d.package_unit AS packageUnit,
			       s.rep_price AS repPrice, s.min_price AS minPrice, s.max_price AS maxPrice, s.avg_price AS avgPrice,
			       s.report_count AS reportCount, s.last_reported_at AS lastReportedAt,
			       ROUND(nat.national_avg)::int AS nationalAvgPrice
			FROM pharmacy_drug_price_stat s
			JOIN drug d ON d.id = s.drug_id
			JOIN (
			    SELECT drug_id, AVG(rep_price) AS national_avg
			    FROM pharmacy_drug_price_stat
			    GROUP BY drug_id
			) nat ON nat.drug_id = s.drug_id
			WHERE s.pharmacy_id = :pharmacyId
			ORDER BY s.rep_price ASC
			""",
		nativeQuery = true)
	List<DrugPriceProjection> findDrugPricesByPharmacyId(@Param("pharmacyId") long pharmacyId);

	// T-20: 가격 이력. HIDDEN 제보는 제외하되 flagged=true(이상치)는 포함해 프론트에서 점선으로 표시한다.
	@Query(
		value = """
			SELECT purchased_at AS purchasedAt, price AS price, flagged AS flagged
			FROM price_report
			WHERE pharmacy_id = :pharmacyId AND drug_id = :drugId
			  AND status <> 'HIDDEN'
			  AND purchased_at >= :fromDate
			ORDER BY purchased_at ASC
			""",
		nativeQuery = true)
	List<PriceHistoryProjection> findHistory(
		@Param("pharmacyId") long pharmacyId, @Param("drugId") long drugId, @Param("fromDate") LocalDate fromDate);

	interface PharmacySummaryProjection {
		Long getId();
		String getName();
		String getAddressRoad();
		Double getLat();
		Double getLng();
		String getPhone();
		String getRegionCode();
		String getRegionSido();
		String getRegionSigungu();
	}

	interface DrugPriceProjection {
		Long getDrugId();
		String getDisplayName();
		String getPackageUnit();
		Integer getRepPrice();
		Integer getMinPrice();
		Integer getMaxPrice();
		Integer getAvgPrice();
		Integer getReportCount();
		LocalDate getLastReportedAt();
		Integer getNationalAvgPrice();
	}

	interface PriceHistoryProjection {
		LocalDate getPurchasedAt();
		Integer getPrice();
		Boolean getFlagged();
	}
}
