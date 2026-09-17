package com.pharmaprice.admin.service;

import com.pharmaprice.admin.dto.AdminStatsResponse.DrugStats;
import com.pharmaprice.admin.dto.AdminStatsResponse.Overview;
import com.pharmaprice.admin.dto.AdminStatsResponse.PriceGaps;
import com.pharmaprice.admin.dto.AdminStatsResponse.RegionStats;
import com.pharmaprice.admin.repository.AdminStatsRepository;
import com.pharmaprice.admin.repository.AdminStatsRepository.DailyTrendRow;
import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.drug.repository.DrugRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int HISTOGRAM_BUCKET_SIZE = 500;
	private static final int DEFAULT_PRICE_GAPS_LIMIT = 10;
	private static final int MAX_PRICE_GAPS_LIMIT = 50;

	private final AdminStatsRepository adminStatsRepository;
	private final DrugRepository drugRepository;

	public Overview overview() {
		long pharmacyCount = adminStatsRepository.countActivePharmacies();
		long drugCount = adminStatsRepository.countDrugs();
		long reportCount = adminStatsRepository.countReports();
		long userCount = adminStatsRepository.countUsers();
		long coveredPairCount = adminStatsRepository.countCoveredPairs();
		long totalPairCount = adminStatsRepository.countTotalPairs();
		long flaggedReportCount = adminStatsRepository.countFlaggedReports();

		double coverageRate = totalPairCount == 0 ? 0.0 : (double) coveredPairCount / totalPairCount;

		Overview.Totals totals = new Overview.Totals(pharmacyCount, drugCount, reportCount, userCount, coveredPairCount);
		List<Overview.TrendPoint> recentTrend = buildRecentTrend(adminStatsRepository.findRecentTrend());

		return new Overview(totals, recentTrend, flaggedReportCount, coverageRate);
	}

	// DB에 값이 없는 날짜는 0건으로 채워 항상 최근 7일 전체를 반환한다
	private List<Overview.TrendPoint> buildRecentTrend(List<DailyTrendRow> rows) {
		Map<LocalDate, Long> countByDate = new LinkedHashMap<>();
		for (DailyTrendRow row : rows) {
			countByDate.put(row.getReportDate(), row.getReportCount());
		}
		LocalDate today = LocalDate.now(KST);
		return java.util.stream.IntStream.rangeClosed(0, 6)
			.mapToObj(i -> today.minusDays(6 - i))
			.map(date -> new Overview.TrendPoint(date, countByDate.getOrDefault(date, 0L)))
			.toList();
	}

	public RegionStats regions(String regionCode, Long drugId, String sido) {
		List<RegionStats.Row> rows = adminStatsRepository.findRegionStats(regionCode, drugId, sido).stream()
			.map(r -> new RegionStats.Row(
				new RegionStats.Region(r.getRegionCode(), r.getSido(), r.getSigungu()),
				new RegionStats.Drug(r.getDrugId(), r.getDrugDisplayName()),
				r.getAvgPrice(), r.getMinPrice(), r.getMaxPrice(), r.getPharmacyCount(), r.getReportCount()))
			.toList();
		return new RegionStats(rows);
	}

	public DrugStats drugStats(long drugId) {
		Drug drug = drugRepository.findById(drugId).orElseThrow(() -> new NoSuchElementException("DRUG_NOT_FOUND"));

		List<DrugStats.Bucket> distribution = adminStatsRepository.findPriceHistogram(drugId).stream()
			.map(r -> new DrugStats.Bucket(r.getBucketFrom(), r.getBucketFrom() + HISTOGRAM_BUCKET_SIZE, r.getBucketCount()))
			.toList();

		List<DrugStats.RegionAvg> byRegion = adminStatsRepository.findRegionAveragesByDrug(drugId).stream()
			.map(r -> new DrugStats.RegionAvg(r.getSido(), r.getSigungu(), r.getAvgPrice(), r.getPharmacyCount()))
			.toList();

		var nationalRow = adminStatsRepository.findNationalStat(drugId);
		DrugStats.National national = nationalRow == null || nationalRow.getAvg() == null
			? new DrugStats.National(0, 0, 0, 0, 0)
			: new DrugStats.National(
				nationalRow.getAvg(), nationalRow.getMedian(), nationalRow.getMinPrice(), nationalRow.getMaxPrice(),
				nationalRow.getStdDev());

		return new DrugStats(
			new DrugStats.Drug(drug.getId(), drug.getDisplayName(), drug.getPackageUnit()),
			distribution, byRegion, national);
	}

	public PriceGaps priceGaps(int limit) {
		int effectiveLimit = limit <= 0 ? DEFAULT_PRICE_GAPS_LIMIT : Math.min(limit, MAX_PRICE_GAPS_LIMIT);
		List<PriceGaps.Row> rows = adminStatsRepository.findPriceGaps(effectiveLimit).stream()
			.map(r -> new PriceGaps.Row(
				new PriceGaps.Drug(r.getDrugId(), r.getDrugDisplayName()),
				new PriceGaps.RegionPrice(r.getCheapestSido(), r.getCheapestSigungu(), r.getCheapestAvgPrice()),
				new PriceGaps.RegionPrice(r.getPriciestSido(), r.getPriciestSigungu(), r.getPriciestAvgPrice()),
				r.getGap(), r.getGapPct().doubleValue()))
			.toList();
		return new PriceGaps(rows);
	}
}
