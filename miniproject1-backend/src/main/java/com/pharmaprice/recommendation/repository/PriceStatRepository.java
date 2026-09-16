package com.pharmaprice.recommendation.repository;

import com.pharmaprice.recommendation.domain.PharmacyDrugPriceStat;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceStatRepository extends JpaRepository<PharmacyDrugPriceStat, Long> {

	Optional<PharmacyDrugPriceStat> findByPharmacyIdAndDrugId(long pharmacyId, long drugId);

	// DATABASE.md §5.2: IQR 이상치 제거 후 중앙값을 대표가격으로 산출
	@Query(value = """
		WITH valid AS (
		  SELECT price, purchased_at
		  FROM price_report
		  WHERE pharmacy_id = :pharmacyId
		    AND drug_id = :drugId
		    AND status = 'ACTIVE'
		    AND flagged = false
		    AND purchased_at >= CURRENT_DATE - :windowDays
		),
		q AS (
		  SELECT
		    percentile_cont(0.25) WITHIN GROUP (ORDER BY price) AS q1,
		    percentile_cont(0.75) WITHIN GROUP (ORDER BY price) AS q3,
		    count(*) AS n
		  FROM valid
		),
		trimmed AS (
		  SELECT v.price
		  FROM valid v CROSS JOIN q
		  WHERE q.n < 4
		     OR v.price BETWEEN q.q1 - 1.5 * (q.q3 - q.q1)
		                    AND q.q3 + 1.5 * (q.q3 - q.q1)
		)
		SELECT
		  percentile_cont(0.5) WITHIN GROUP (ORDER BY price)::int AS repPrice,
		  MIN(price)::int  AS minPrice,
		  MAX(price)::int  AS maxPrice,
		  AVG(price)::int  AS avgPrice,
		  COUNT(*)::int    AS reportCount,
		  (SELECT MAX(purchased_at) FROM valid) AS lastReportedAt
		FROM trimmed
		""", nativeQuery = true)
	Optional<RecalculatedStat> calculateStat(
		@Param("pharmacyId") long pharmacyId, @Param("drugId") long drugId, @Param("windowDays") int windowDays);

	@Modifying
	@Query(value = """
		INSERT INTO pharmacy_drug_price_stat
		  (pharmacy_id, drug_id, rep_price, min_price, max_price, avg_price, report_count, last_reported_at, window_days, calculated_at)
		VALUES (:pharmacyId, :drugId, :repPrice, :minPrice, :maxPrice, :avgPrice, :reportCount, :lastReportedAt, :windowDays, now())
		ON CONFLICT (pharmacy_id, drug_id) DO UPDATE SET
		  rep_price = EXCLUDED.rep_price,
		  min_price = EXCLUDED.min_price,
		  max_price = EXCLUDED.max_price,
		  avg_price = EXCLUDED.avg_price,
		  report_count = EXCLUDED.report_count,
		  last_reported_at = EXCLUDED.last_reported_at,
		  window_days = EXCLUDED.window_days,
		  calculated_at = now()
		""", nativeQuery = true)
	void upsert(
		@Param("pharmacyId") long pharmacyId, @Param("drugId") long drugId,
		@Param("repPrice") int repPrice, @Param("minPrice") int minPrice, @Param("maxPrice") int maxPrice,
		@Param("avgPrice") int avgPrice, @Param("reportCount") int reportCount,
		@Param("lastReportedAt") LocalDate lastReportedAt, @Param("windowDays") short windowDays);

	@Modifying
	@Query("DELETE FROM PharmacyDrugPriceStat s WHERE s.pharmacy.id = :pharmacyId AND s.drug.id = :drugId")
	void deleteByPharmacyIdAndDrugId(@Param("pharmacyId") long pharmacyId, @Param("drugId") long drugId);

	interface RecalculatedStat {
		Integer getRepPrice();
		Integer getMinPrice();
		Integer getMaxPrice();
		Integer getAvgPrice();
		Integer getReportCount();
		LocalDate getLastReportedAt();
	}
}
