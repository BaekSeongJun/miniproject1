package com.pharmaprice.admin.repository;

import com.pharmaprice.pharmacy.domain.Pharmacy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AdminStatsRepository extends Repository<Pharmacy, Long> {

	@Query(value = "SELECT COUNT(*) FROM pharmacy WHERE is_active = true", nativeQuery = true)
	long countActivePharmacies();

	@Query(value = "SELECT COUNT(*) FROM drug", nativeQuery = true)
	long countDrugs();

	@Query(value = "SELECT COUNT(*) FROM price_report", nativeQuery = true)
	long countReports();

	@Query(value = "SELECT COUNT(*) FROM app_user", nativeQuery = true)
	long countUsers();

	@Query(value = "SELECT COUNT(*) FROM pharmacy_drug_price_stat", nativeQuery = true)
	long countCoveredPairs();

	@Query(value = "SELECT (SELECT COUNT(*) FROM pharmacy WHERE is_active = true) * (SELECT COUNT(*) FROM drug)",
		nativeQuery = true)
	long countTotalPairs();

	@Query(value = "SELECT COUNT(*) FROM price_report WHERE flagged = true", nativeQuery = true)
	long countFlaggedReports();

	// 최근 7일(오늘 포함) 제보 등록 추이. KST 자정 기준으로 날짜를 끊는다.
	@Query(value = """
		SELECT (created_at AT TIME ZONE 'Asia/Seoul')::date AS reportDate, COUNT(*) AS reportCount
		FROM price_report
		WHERE created_at >= (CURRENT_DATE - INTERVAL '6 days') AT TIME ZONE 'Asia/Seoul'
		GROUP BY reportDate
		ORDER BY reportDate
		""", nativeQuery = true)
	List<DailyTrendRow> findRecentTrend();

	// §5.3 지역별 가격 통계 (F4-3)
	// native query는 파라미터가 값으로만 바인딩되어 ORDER BY :sortCol이 동작하지 않으므로 CASE WHEN으로 분기한다.
	// sortCol/sortDir 값 자체는 서비스 레이어 화이트리스트로 검증된 것만 들어온다.
	@Query(
		value = """
			SELECT r.code AS regionCode, r.sido AS sido, r.sigungu AS sigungu,
			       d.id AS drugId, d.display_name AS drugDisplayName,
			       ROUND(AVG(s.rep_price))::int AS avgPrice,
			       MIN(s.rep_price) AS minPrice,
			       MAX(s.rep_price) AS maxPrice,
			       COUNT(DISTINCT s.pharmacy_id) AS pharmacyCount,
			       SUM(s.report_count) AS reportCount
			FROM pharmacy_drug_price_stat s
			JOIN pharmacy p ON p.id = s.pharmacy_id
			JOIN region   r ON r.code = p.region_code
			JOIN drug     d ON d.id = s.drug_id
			WHERE (:regionCode IS NULL OR p.region_code = :regionCode)
			  AND (:drugId     IS NULL OR s.drug_id = :drugId)
			  AND (:sido       IS NULL OR r.sido = :sido)
			GROUP BY r.code, r.sido, r.sigungu, d.id, d.display_name
			ORDER BY
			  CASE WHEN :sortCol = 'avgPrice'      AND :sortDir = 'ASC'  THEN AVG(s.rep_price) END ASC,
			  CASE WHEN :sortCol = 'avgPrice'      AND :sortDir = 'DESC' THEN AVG(s.rep_price) END DESC,
			  CASE WHEN :sortCol = 'minPrice'      AND :sortDir = 'ASC'  THEN MIN(s.rep_price) END ASC,
			  CASE WHEN :sortCol = 'minPrice'      AND :sortDir = 'DESC' THEN MIN(s.rep_price) END DESC,
			  CASE WHEN :sortCol = 'maxPrice'      AND :sortDir = 'ASC'  THEN MAX(s.rep_price) END ASC,
			  CASE WHEN :sortCol = 'maxPrice'      AND :sortDir = 'DESC' THEN MAX(s.rep_price) END DESC,
			  CASE WHEN :sortCol = 'pharmacyCount' AND :sortDir = 'ASC'  THEN COUNT(DISTINCT s.pharmacy_id) END ASC,
			  CASE WHEN :sortCol = 'pharmacyCount' AND :sortDir = 'DESC' THEN COUNT(DISTINCT s.pharmacy_id) END DESC,
			  CASE WHEN :sortCol = 'reportCount'   AND :sortDir = 'ASC'  THEN SUM(s.report_count) END ASC,
			  CASE WHEN :sortCol = 'reportCount'   AND :sortDir = 'DESC' THEN SUM(s.report_count) END DESC,
			  r.sido, r.sigungu, d.display_name
			""",
		countQuery = """
			SELECT COUNT(*) FROM (
			  SELECT 1
			  FROM pharmacy_drug_price_stat s
			  JOIN pharmacy p ON p.id = s.pharmacy_id
			  JOIN region   r ON r.code = p.region_code
			  JOIN drug     d ON d.id = s.drug_id
			  WHERE (:regionCode IS NULL OR p.region_code = :regionCode)
			    AND (:drugId     IS NULL OR s.drug_id = :drugId)
			    AND (:sido       IS NULL OR r.sido = :sido)
			  GROUP BY r.code, d.id
			) counted
			""",
		nativeQuery = true)
	Page<RegionStatRow> findRegionStats(
		@Param("regionCode") String regionCode, @Param("drugId") Long drugId, @Param("sido") String sido,
		@Param("sortCol") String sortCol, @Param("sortDir") String sortDir, Pageable pageable);

	// 가격 히스토그램 (500원 버킷)
	@Query(value = """
		SELECT (width_bucket(rep_price, 0, 100000, 200) - 1) * 500 AS bucketFrom,
		       COUNT(*) AS bucketCount
		FROM pharmacy_drug_price_stat
		WHERE drug_id = :drugId
		GROUP BY bucketFrom
		ORDER BY bucketFrom
		""", nativeQuery = true)
	List<HistogramRow> findPriceHistogram(@Param("drugId") long drugId);

	// 약품별 지역 평균가
	@Query(value = """
		SELECT r.sido AS sido, r.sigungu AS sigungu,
		       ROUND(AVG(s.rep_price))::int AS avgPrice,
		       COUNT(DISTINCT s.pharmacy_id) AS pharmacyCount
		FROM pharmacy_drug_price_stat s
		JOIN pharmacy p ON p.id = s.pharmacy_id
		JOIN region   r ON r.code = p.region_code
		WHERE s.drug_id = :drugId
		GROUP BY r.sido, r.sigungu
		ORDER BY r.sido, r.sigungu
		""", nativeQuery = true)
	List<RegionAvgRow> findRegionAveragesByDrug(@Param("drugId") long drugId);

	// 약품 전국 통계 (avg/median/min/max/stdDev)
	@Query(value = """
		SELECT AVG(rep_price)::float AS avg,
		       percentile_cont(0.5) WITHIN GROUP (ORDER BY rep_price)::float AS median,
		       MIN(rep_price) AS minPrice,
		       MAX(rep_price) AS maxPrice,
		       stddev_pop(rep_price)::float AS stdDev
		FROM pharmacy_drug_price_stat
		WHERE drug_id = :drugId
		""", nativeQuery = true)
	NationalStatRow findNationalStat(@Param("drugId") long drugId);

	// §5.4 지역 간 가격 격차 Top N (표본 3건 미만 지역 제외)
	@Query(value = """
		WITH by_region AS (
		  SELECT s.drug_id, p.region_code, AVG(s.rep_price) AS region_avg
		  FROM pharmacy_drug_price_stat s
		  JOIN pharmacy p ON p.id = s.pharmacy_id
		  GROUP BY s.drug_id, p.region_code
		  HAVING COUNT(*) >= 3
		),
		ranked AS (
		  SELECT b.drug_id,
		         (ARRAY_AGG(b.region_code ORDER BY b.region_avg ASC))[1]  AS cheapest_region_code,
		         (ARRAY_AGG(b.region_code ORDER BY b.region_avg DESC))[1] AS priciest_region_code,
		         MIN(b.region_avg) AS cheapest_avg,
		         MAX(b.region_avg) AS priciest_avg
		  FROM by_region b
		  GROUP BY b.drug_id
		  HAVING COUNT(*) >= 3
		)
		SELECT d.id AS drugId, d.display_name AS drugDisplayName,
		       cr.sido AS cheapestSido, cr.sigungu AS cheapestSigungu, ROUND(ranked.cheapest_avg)::int AS cheapestAvgPrice,
		       pr.sido AS priciestSido, pr.sigungu AS priciestSigungu, ROUND(ranked.priciest_avg)::int AS priciestAvgPrice,
		       ROUND(ranked.priciest_avg - ranked.cheapest_avg)::int AS gap,
		       ROUND((ranked.priciest_avg - ranked.cheapest_avg) / ranked.cheapest_avg * 100, 1) AS gapPct
		FROM ranked
		JOIN drug   d  ON d.id = ranked.drug_id
		JOIN region cr ON cr.code = ranked.cheapest_region_code
		JOIN region pr ON pr.code = ranked.priciest_region_code
		ORDER BY gapPct DESC
		LIMIT :limit
		""", nativeQuery = true)
	List<PriceGapRow> findPriceGaps(@Param("limit") int limit);

	// 약국별 제보 건수 Top N (기간 필터)
	@Query(value = """
		SELECT p.id AS pharmacyId, p.name AS pharmacyName,
		       r.sido AS sido, r.sigungu AS sigungu,
		       COUNT(*) AS reportCount
		FROM price_report pr
		JOIN pharmacy p ON p.id = pr.pharmacy_id
		LEFT JOIN region r ON r.code = p.region_code
		WHERE pr.status = 'ACTIVE'
		  AND (CAST(:since AS timestamptz) IS NULL OR pr.created_at >= CAST(:since AS timestamptz))
		GROUP BY p.id, p.name, r.sido, r.sigungu
		ORDER BY reportCount DESC
		LIMIT :limit
		""", nativeQuery = true)
	List<PharmacyRankRow> findTopPharmaciesByReportCount(@Param("since") Instant since, @Param("limit") int limit);

	// 제보자 순위 (user_id INNER JOIN이라 익명 제보 자동 제외, 기간 필터)
	@Query(value = """
		SELECT u.id AS userId, u.nickname AS nickname, COUNT(*) AS reportCount
		FROM price_report pr
		JOIN app_user u ON u.id = pr.user_id
		WHERE pr.status = 'ACTIVE'
		  AND (CAST(:since AS timestamptz) IS NULL OR pr.created_at >= CAST(:since AS timestamptz))
		GROUP BY u.id, u.nickname
		ORDER BY reportCount DESC
		LIMIT :limit
		""", nativeQuery = true)
	List<ReporterRankRow> findTopReporters(@Param("since") Instant since, @Param("limit") int limit);

	// 약품별 제보 건수 순위 (기간 필터)
	@Query(value = """
		SELECT d.id AS drugId, d.display_name AS drugDisplayName, COUNT(*) AS reportCount
		FROM price_report pr
		JOIN drug d ON d.id = pr.drug_id
		WHERE pr.status = 'ACTIVE'
		  AND (CAST(:since AS timestamptz) IS NULL OR pr.created_at >= CAST(:since AS timestamptz))
		GROUP BY d.id, d.display_name
		ORDER BY reportCount DESC
		LIMIT :limit
		""", nativeQuery = true)
	List<DrugReportRankRow> findTopDrugsByReportCount(@Param("since") Instant since, @Param("limit") int limit);

	interface DailyTrendRow {
		LocalDate getReportDate();
		Long getReportCount();
	}

	interface RegionStatRow {
		String getRegionCode();
		String getSido();
		String getSigungu();
		Long getDrugId();
		String getDrugDisplayName();
		Integer getAvgPrice();
		Integer getMinPrice();
		Integer getMaxPrice();
		Long getPharmacyCount();
		Long getReportCount();
	}

	interface HistogramRow {
		Integer getBucketFrom();
		Long getBucketCount();
	}

	interface RegionAvgRow {
		String getSido();
		String getSigungu();
		Integer getAvgPrice();
		Long getPharmacyCount();
	}

	interface NationalStatRow {
		Double getAvg();
		Double getMedian();
		Integer getMinPrice();
		Integer getMaxPrice();
		Double getStdDev();
	}

	interface PriceGapRow {
		Long getDrugId();
		String getDrugDisplayName();
		String getCheapestSido();
		String getCheapestSigungu();
		Integer getCheapestAvgPrice();
		String getPriciestSido();
		String getPriciestSigungu();
		Integer getPriciestAvgPrice();
		Integer getGap();
		BigDecimal getGapPct();
	}

	interface PharmacyRankRow {
		Long getPharmacyId();
		String getPharmacyName();
		String getSido();
		String getSigungu();
		Long getReportCount();
	}

	interface ReporterRankRow {
		Long getUserId();
		String getNickname();
		Long getReportCount();
	}

	interface DrugReportRankRow {
		Long getDrugId();
		String getDrugDisplayName();
		Long getReportCount();
	}
}
