package com.pharmaprice.recommendation.repository;

import com.pharmaprice.drug.domain.Drug;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface SearchLogRepository extends Repository<Drug, Long> {

	@Modifying
	@Query(value = "INSERT INTO search_log (drug_id) VALUES (:drugId)", nativeQuery = true)
	void insert(@Param("drugId") long drugId);

	// 약품별 검색 순위 (기간 필터)
	@Query(value = """
		SELECT d.id AS drugId, d.display_name AS drugDisplayName, COUNT(*) AS searchCount
		FROM search_log s
		JOIN drug d ON d.id = s.drug_id
		WHERE (CAST(:since AS timestamptz) IS NULL OR s.created_at >= CAST(:since AS timestamptz))
		GROUP BY d.id, d.display_name
		ORDER BY searchCount DESC
		LIMIT :limit
		""", nativeQuery = true)
	List<DrugSearchRankRow> findTopDrugsBySearchCount(@Param("since") Instant since, @Param("limit") int limit);

	interface DrugSearchRankRow {
		Long getDrugId();
		String getDrugDisplayName();
		Long getSearchCount();
	}
}
