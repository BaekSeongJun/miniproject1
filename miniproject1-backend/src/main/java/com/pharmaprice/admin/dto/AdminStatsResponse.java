package com.pharmaprice.admin.dto;

import java.time.LocalDate;
import java.util.List;

public class AdminStatsResponse {

	public record Overview(Totals totals, List<TrendPoint> recentTrend, long flaggedReportCount, double coverageRate) {
		public record Totals(
			long pharmacyCount, long drugCount, long reportCount, long userCount, long coveredPairCount) {}

		public record TrendPoint(LocalDate date, long reportCount) {}
	}

	public record RegionStats(List<Row> rows) {
		public record Row(Region region, Drug drug, int avgPrice, int minPrice, int maxPrice,
			long pharmacyCount, long reportCount) {}

		public record Region(String code, String sido, String sigungu) {}

		public record Drug(long id, String displayName) {}
	}

	public record DrugStats(
		Drug drug, List<Bucket> distribution, List<RegionAvg> byRegion, National national) {
		public record Drug(long id, String displayName, String packageUnit) {}

		public record Bucket(int bucketFrom, int bucketTo, long count) {}

		public record RegionAvg(String sido, String sigungu, int avgPrice, long pharmacyCount) {}

		public record National(double avg, double median, int min, int max, double stdDev) {}
	}

	public record PriceGaps(List<Row> rows) {
		public record Row(Drug drug, RegionPrice cheapestRegion, RegionPrice priciestRegion, int gap, double gapPct) {}

		public record Drug(long id, String displayName) {}

		public record RegionPrice(String sido, String sigungu, int avgPrice) {}
	}
}
