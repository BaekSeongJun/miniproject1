package com.pharmaprice.recommendation.service;

import com.pharmaprice.common.config.RecommendationProperties;
import com.pharmaprice.recommendation.dto.Badge;
import com.pharmaprice.recommendation.dto.Candidate;
import com.pharmaprice.recommendation.dto.ScoreBreakdown;
import com.pharmaprice.recommendation.dto.ScoredCandidate;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScoreCalculatorImpl implements ScoreCalculator {

	private static final int SCALE = 4;

	private final RecommendationProperties properties;

	@Override
	public List<ScoredCandidate> rank(List<Candidate> candidates, int radiusM, LocalDate today) {
		if (candidates.isEmpty()) {
			return List.of();
		}

		int pMin = candidates.stream().mapToInt(Candidate::repPrice).min().orElseThrow();
		int pMax = candidates.stream().mapToInt(Candidate::repPrice).max().orElseThrow();
		double minDistance = candidates.stream().mapToDouble(Candidate::distanceM).min().orElseThrow();

		RecommendationProperties.Weights weights = properties.weights();

		List<ScoredCandidate> scored = candidates.stream()
			.map(c -> score(c, pMin, pMax, radiusM, today, weights, minDistance))
			.sorted(Comparator
				.comparingDouble((ScoredCandidate sc) -> sc.score()).reversed()
				.thenComparingInt(sc -> sc.candidate().repPrice())
				.thenComparingDouble(sc -> sc.candidate().distanceM())
				.thenComparingLong(sc -> sc.candidate().pharmacyId()))
			.toList();

		if (!scored.isEmpty()) {
			ScoredCandidate top = scored.get(0);
			scored = scored.stream()
				.map(sc -> sc == top ? withBadge(sc, Badge.LOWEST_PRICE) : sc)
				.toList();
		}

		return scored;
	}

	private ScoredCandidate score(Candidate c, int pMin, int pMax, int radiusM, LocalDate today,
			RecommendationProperties.Weights weights, double minDistance) {
		double priceScore = (pMax == pMin) ? 1.0 : (double) (pMax - c.repPrice()) / (pMax - pMin);
		double distanceScore = clamp(1 - c.distanceM() / radiusM, 0, 1);

		long ageDays = ChronoUnit.DAYS.between(c.lastReportedAt(), today);
		double freshnessScore = Math.pow(0.5, ageDays / (double) properties.freshnessHalfLifeDays());

		double score = weights.price() * priceScore
			+ weights.distance() * distanceScore
			+ weights.freshness() * freshnessScore;

		ScoreBreakdown breakdown = new ScoreBreakdown(
			round(priceScore), round(distanceScore), round(freshnessScore),
			new ScoreBreakdown.Weights(weights.price(), weights.distance(), weights.freshness()));

		List<Badge> badges = new ArrayList<>();
		if (c.reportCount() == 1) {
			badges.add(Badge.LOW_CONFIDENCE);
		}
		if (ageDays > 30) {
			badges.add(Badge.STALE_DATA);
		}
		if (c.distanceM() == minDistance) {
			badges.add(Badge.NEAREST);
		}

		return new ScoredCandidate(c, round(score), breakdown, badges);
	}

	private ScoredCandidate withBadge(ScoredCandidate sc, Badge badge) {
		List<Badge> badges = new ArrayList<>();
		badges.add(badge);
		badges.addAll(sc.badges());
		return new ScoredCandidate(sc.candidate(), sc.score(), sc.breakdown(), badges);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double round(double value) {
		double factor = Math.pow(10, SCALE);
		return Math.round(value * factor) / factor;
	}
}
