package com.pharmaprice.pharmacy.repository;

import com.pharmaprice.pharmacy.domain.Region;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface RegionQueryRepository extends Repository<Region, String> {

	// T-14: 시군구별 활성 약국 수를 조인 한 번으로 집계 (N+1 방지)
	@Query(
		value = """
			SELECT r.code AS code, r.sido AS sido, r.sigungu AS sigungu,
			       r.center_lat AS centerLat, r.center_lng AS centerLng,
			       COUNT(p.id) FILTER (WHERE p.is_active = true) AS pharmacyCount
			FROM region r
			LEFT JOIN pharmacy p ON p.region_code = r.code
			GROUP BY r.code
			ORDER BY r.sido, r.sigungu
			""",
		nativeQuery = true)
	List<RegionProjection> findAllWithPharmacyCount();

	interface RegionProjection {
		String getCode();
		String getSido();
		String getSigungu();
		Double getCenterLat();
		Double getCenterLng();
		Long getPharmacyCount();
	}
}
