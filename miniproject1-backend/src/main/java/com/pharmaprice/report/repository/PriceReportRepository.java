package com.pharmaprice.report.repository;

import com.pharmaprice.report.domain.PriceReport;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceReportRepository extends JpaRepository<PriceReport, Long> {

	// 이상치 판정 기준: 약국 구분 없이 해당 약품 전체의 유효 제보 중앙값
	@Query(value = """
		SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY price)
		FROM price_report
		WHERE drug_id = :drugId AND status = 'ACTIVE' AND flagged = false
		""", nativeQuery = true)
	Optional<BigDecimal> findDrugMedianPrice(@Param("drugId") long drugId);
}
