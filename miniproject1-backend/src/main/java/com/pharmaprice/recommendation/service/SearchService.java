package com.pharmaprice.recommendation.service;

import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.drug.repository.DrugRepository;
import com.pharmaprice.pharmacy.domain.Region;
import com.pharmaprice.pharmacy.repository.RegionRepository;
import com.pharmaprice.recommendation.distance.BoundingBox;
import com.pharmaprice.recommendation.distance.CoordinateValidator;
import com.pharmaprice.recommendation.distance.DistanceCalculator;
import com.pharmaprice.recommendation.dto.Candidate;
import com.pharmaprice.recommendation.dto.ScoredCandidate;
import com.pharmaprice.recommendation.dto.SearchResponse;
import com.pharmaprice.recommendation.dto.SearchResponse.ResultItem;
import com.pharmaprice.recommendation.dto.SearchResponse.Suggestion;
import com.pharmaprice.recommendation.repository.SearchQueryRepository;
import com.pharmaprice.recommendation.repository.SearchQueryRepository.CandidateRowProjection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

	private static final Logger log = LoggerFactory.getLogger(SearchService.class);
	private static final List<Integer> RADIUS_STEPS = List.of(500, 1000, 2000, 5000);
	private static final Set<Integer> ALLOWED_RADIUS = Set.copyOf(RADIUS_STEPS);
	private static final int MAX_LIMIT = 50;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final DrugRepository drugRepository;
	private final RegionRepository regionRepository;
	private final SearchQueryRepository searchQueryRepository;
	private final DistanceCalculator distanceCalculator;
	private final ScoreCalculator scoreCalculator;

	public SearchResponse search(Long drugId, Double lat, Double lng, String regionCode,
			int radius, String sort, int limit) {
		if (drugId == null) {
			throw new IllegalArgumentException("VALIDATION_FAILED: drugId is required");
		}
		boolean hasCoord = lat != null && lng != null;
		boolean hasRegion = regionCode != null && !regionCode.isBlank();
		if (!hasCoord && !hasRegion) {
			throw new IllegalArgumentException("VALIDATION_FAILED: lat+lng or regionCode is required");
		}
		if (!ALLOWED_RADIUS.contains(radius)) {
			throw new IllegalArgumentException("INVALID_RADIUS: " + radius);
		}

		double effLat;
		double effLng;
		String locationSource;
		if (hasCoord) {
			if (!CoordinateValidator.isValid(lat, lng)) {
				throw new IllegalArgumentException("INVALID_COORDINATE: lat=" + lat + ", lng=" + lng);
			}
			effLat = lat;
			effLng = lng;
			locationSource = "GPS";
		} else {
			Region region = regionRepository.findById(regionCode)
				.orElseThrow(() -> new NoSuchElementException("REGION_NOT_FOUND"));
			effLat = region.getCenterLat();
			effLng = region.getCenterLng();
			locationSource = "REGION";
		}

		Drug drug = drugRepository.findById(drugId)
			.orElseThrow(() -> new NoSuchElementException("DRUG_NOT_FOUND"));

		List<CandidateRowProjection> filtered = findWithinRadius(drugId, effLat, effLng, radius);

		SearchResponse.Drug drugDto = new SearchResponse.Drug(
			drug.getId(), drug.getDisplayName(), drug.getPackageUnit(), drug.getImageUrl());
		SearchResponse.Query queryDto = new SearchResponse.Query(effLat, effLng, radius, sort, locationSource);

		if (filtered.isEmpty()) {
			Suggestion suggestion = computeSuggestion(drugId, effLat, effLng, radius);
			SearchResponse.Summary emptySummary = new SearchResponse.Summary(0, null, null, null, null);
			return new SearchResponse(drugDto, queryDto, emptySummary, "SEED", List.of(), suggestion);
		}

		LocalDate today = LocalDate.now(KST);
		Map<Long, CandidateRowProjection> rowById = filtered.stream()
			.collect(java.util.stream.Collectors.toMap(CandidateRowProjection::getPharmacyId, Function.identity()));

		List<Candidate> candidates = filtered.stream()
			.map(r -> new Candidate(r.getPharmacyId(), r.getRepPrice(), distanceMeters(effLat, effLng, r),
				r.getLastReportedAt(), r.getReportCount()))
			.toList();

		List<ScoredCandidate> ranked = scoreCalculator.rank(candidates, radius, today);
		ranked.forEach(sc -> log.debug("search rank drugId={} pharmacyId={} score={} breakdown={}",
			drugId, sc.candidate().pharmacyId(), sc.score(), sc.breakdown()));

		SearchResponse.Summary summary = computeSummary(candidates);

		List<ScoredCandidate> sorted = applySort(ranked, sort);
		List<ScoredCandidate> limited = sorted.size() > Math.min(limit, MAX_LIMIT)
			? sorted.subList(0, Math.min(limit, MAX_LIMIT))
			: sorted;

		String dataSource = computeDataSource(drugId, limited.stream().map(sc -> sc.candidate().pharmacyId()).toList());

		List<ResultItem> results = toResultItems(limited, rowById, summary.candidateAvgPrice(), today);

		return new SearchResponse(drugDto, queryDto, summary, dataSource, results, null);
	}

	private List<CandidateRowProjection> findWithinRadius(long drugId, double lat, double lng, int radius) {
		BoundingBox box = distanceCalculator.boundingBox(lat, lng, radius);
		List<CandidateRowProjection> rows = searchQueryRepository.findCandidates(
			drugId, box.minLat(), box.maxLat(), box.minLng(), box.maxLng());
		return rows.stream()
			.filter(r -> distanceMeters(lat, lng, r) <= radius)
			.toList();
	}

	private double distanceMeters(double lat, double lng, CandidateRowProjection r) {
		return distanceCalculator.distanceMeters(lat, lng, r.getLat(), r.getLng());
	}

	private Suggestion computeSuggestion(long drugId, double lat, double lng, int currentRadius) {
		int idx = RADIUS_STEPS.indexOf(currentRadius);
		if (idx == RADIUS_STEPS.size() - 1) {
			return null;
		}
		int nextRadius = RADIUS_STEPS.get(idx + 1);
		int count = findWithinRadius(drugId, lat, lng, nextRadius).size();
		return new Suggestion("EXPAND_RADIUS", nextRadius, count);
	}

	private SearchResponse.Summary computeSummary(List<Candidate> candidates) {
		int min = candidates.stream().mapToInt(Candidate::repPrice).min().orElseThrow();
		int max = candidates.stream().mapToInt(Candidate::repPrice).max().orElseThrow();
		int avg = (int) Math.round(candidates.stream().mapToInt(Candidate::repPrice).average().orElseThrow());
		return new SearchResponse.Summary(candidates.size(), avg, min, max, max - min);
	}

	private List<ScoredCandidate> applySort(List<ScoredCandidate> ranked, String sort) {
		Comparator<ScoredCandidate> comparator = switch (sort) {
			case "PRICE" -> Comparator
				.comparingInt((ScoredCandidate sc) -> sc.candidate().repPrice())
				.thenComparing(Comparator.comparingDouble((ScoredCandidate sc) -> sc.score()).reversed())
				.thenComparingDouble(sc -> sc.candidate().distanceM())
				.thenComparingLong(sc -> sc.candidate().pharmacyId());
			case "DISTANCE" -> Comparator
				.comparingDouble((ScoredCandidate sc) -> sc.candidate().distanceM())
				.thenComparing(Comparator.comparingDouble((ScoredCandidate sc) -> sc.score()).reversed())
				.thenComparingInt(sc -> sc.candidate().repPrice())
				.thenComparingLong(sc -> sc.candidate().pharmacyId());
			default -> null;
		};
		if (comparator == null) {
			return ranked;
		}
		return ranked.stream().sorted(comparator).toList();
	}

	private String computeDataSource(long drugId, List<Long> pharmacyIds) {
		List<String> sources = searchQueryRepository.findDistinctSources(drugId, pharmacyIds);
		if (sources.isEmpty()) {
			return "SEED";
		}
		boolean allSeed = sources.stream().allMatch("SEED"::equals);
		boolean allUser = sources.stream().noneMatch("SEED"::equals);
		if (allSeed) {
			return "SEED";
		}
		if (allUser) {
			return "USER";
		}
		return "MIXED";
	}

	private List<ResultItem> toResultItems(List<ScoredCandidate> limited, Map<Long, CandidateRowProjection> rowById,
			Integer candidateAvgPrice, LocalDate today) {
		List<ResultItem> results = new ArrayList<>();
		for (int i = 0; i < limited.size(); i++) {
			ScoredCandidate sc = limited.get(i);
			CandidateRowProjection row = rowById.get(sc.candidate().pharmacyId());
			int rank = i + 1;
			long daysSinceLastReport = ChronoUnit.DAYS.between(row.getLastReportedAt(), today);
			results.add(new ResultItem(
				rank, rank == 1,
				new ResultItem.Pharmacy(row.getPharmacyId(), row.getName(), row.getAddressRoad(), row.getLat(), row.getLng(), row.getPhone()),
				new ResultItem.Price(
					row.getRepPrice(), row.getMinPrice(), row.getAvgPrice(),
					candidateAvgPrice - row.getRepPrice(), row.getReportCount(),
					row.getLastReportedAt(), daysSinceLastReport),
				Math.round(sc.candidate().distanceM()), sc.score(), sc.breakdown(), sc.badges()));
		}
		return results;
	}
}
