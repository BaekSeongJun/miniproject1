package com.pharmaprice.recommendation.repository;

import com.pharmaprice.pharmacy.domain.Pharmacy;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface SearchQueryRepository extends Repository<Pharmacy, Long> {

	// T-15: 바운딩박스 + 활성 약국 + 해당 drugId 통계 존재 조건으로 후보 조회 (정확 거리 필터는 자바에서)
	@Query(
		value = """
			SELECT p.id AS pharmacyId, p.name AS name, p.address_road AS addressRoad,
			       p.lat AS lat, p.lng AS lng, p.phone AS phone,
			       s.rep_price AS repPrice, s.min_price AS minPrice, s.avg_price AS avgPrice,
			       s.report_count AS reportCount, s.last_reported_at AS lastReportedAt
			FROM pharmacy p
			JOIN pharmacy_drug_price_stat s ON s.pharmacy_id = p.id
			WHERE s.drug_id = :drugId
			  AND p.is_active = true
			  AND p.lat BETWEEN :minLat AND :maxLat
			  AND p.lng BETWEEN :minLng AND :maxLng
			""",
		nativeQuery = true)
	List<CandidateRowProjection> findCandidates(
		@Param("drugId") long drugId,
		@Param("minLat") double minLat, @Param("maxLat") double maxLat,
		@Param("minLng") double minLng, @Param("maxLng") double maxLng);

	// T-15: 응답에 포함된 약국들의 유효 제보 출처를 조회해 dataSource(SEED/MIXED/USER) 판정에 쓴다
	@Query(
		value = """
			SELECT DISTINCT pr.source AS source
			FROM price_report pr
			WHERE pr.drug_id = :drugId
			  AND pr.pharmacy_id IN (:pharmacyIds)
			  AND pr.status = 'ACTIVE'
			  AND pr.flagged = false
			""",
		nativeQuery = true)
	List<String> findDistinctSources(@Param("drugId") long drugId, @Param("pharmacyIds") List<Long> pharmacyIds);

	interface CandidateRowProjection {
		Long getPharmacyId();
		String getName();
		String getAddressRoad();
		Double getLat();
		Double getLng();
		String getPhone();
		Integer getRepPrice();
		Integer getMinPrice();
		Integer getAvgPrice();
		Integer getReportCount();
		LocalDate getLastReportedAt();
	}
}
