package com.pharmaprice.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchResponse(
	Drug drug, Query query, Summary summary, String dataSource,
	List<ResultItem> results, Suggestion suggestion
) {

	public record Drug(long id, String displayName, String packageUnit, String imageUrl) {}

	public record Query(double lat, double lng, int radius, String sort, String locationSource) {}

	public record Summary(
		int resultCount, Integer candidateAvgPrice, Integer candidateMinPrice,
		Integer candidateMaxPrice, Integer maxSaving
	) {}

	public record ResultItem(
		int rank, boolean recommended, Pharmacy pharmacy, Price price,
		long distanceM, double score, ScoreBreakdown scoreBreakdown, List<Badge> badges
	) {
		public record Pharmacy(long id, String name, String addressRoad, double lat, double lng, String phone) {}

		public record Price(
			int repPrice, int minPrice, int avgPrice, int savingVsCandidateAvg,
			int reportCount, LocalDate lastReportedAt, long daysSinceLastReport
		) {}
	}

	public record Suggestion(String type, int recommendedRadius, int estimatedCount) {}
}
