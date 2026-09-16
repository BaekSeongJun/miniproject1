package com.pharmaprice.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmaprice.common.config.RecommendationProperties;
import com.pharmaprice.recommendation.dto.Badge;
import com.pharmaprice.recommendation.dto.Candidate;
import com.pharmaprice.recommendation.dto.ScoredCandidate;
import com.pharmaprice.recommendation.service.ScoreCalculator;
import com.pharmaprice.recommendation.service.ScoreCalculatorImpl;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScoreCalculatorTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);
	private static final int RADIUS_M = 2000;

	private ScoreCalculator scoreCalculator;

	@BeforeEach
	void setUp() {
		RecommendationProperties properties = new RecommendationProperties(
			new RecommendationProperties.Weights(0.60, 0.25, 0.15),
			30, 90, 180,
			new RecommendationProperties.Outlier(1.5, 4));
		scoreCalculator = new ScoreCalculatorImpl(properties);
	}

	private Candidate candidate(long pharmacyId, int repPrice, double distanceM, int ageDays, int reportCount) {
		return new Candidate(pharmacyId, repPrice, distanceM, TODAY.minusDays(ageDays), reportCount);
	}

	@Test
	void 거리와_신선도가_같으면_가격이_싼_쪽이_상위() {
		List<ScoredCandidate> ranked = scoreCalculator.rank(List.of(
			candidate(1, 3000, 500, 5, 3),
			candidate(2, 2000, 500, 5, 3)
		), RADIUS_M, TODAY);

		assertThat(ranked.get(0).candidate().pharmacyId()).isEqualTo(2);
		assertThat(ranked.get(1).candidate().pharmacyId()).isEqualTo(1);
	}

	@Test
	void 가격과_신선도가_같으면_거리가_가까운_쪽이_상위() {
		List<ScoredCandidate> ranked = scoreCalculator.rank(List.of(
			candidate(1, 3000, 1800, 5, 3),
			candidate(2, 3000, 300, 5, 3)
		), RADIUS_M, TODAY);

		assertThat(ranked.get(0).candidate().pharmacyId()).isEqualTo(2);
		assertThat(ranked.get(1).candidate().pharmacyId()).isEqualTo(1);
	}

	@Test
	void 가격과_거리가_같으면_최근_제보한_쪽이_상위() {
		List<ScoredCandidate> ranked = scoreCalculator.rank(List.of(
			candidate(1, 3000, 500, 60, 3),
			candidate(2, 3000, 500, 1, 3)
		), RADIUS_M, TODAY);

		assertThat(ranked.get(0).candidate().pharmacyId()).isEqualTo(2);
		assertThat(ranked.get(1).candidate().pharmacyId()).isEqualTo(1);
	}

	@Test
	void 후보가_1개면_예외없이_priceScore가_1이다() {
		List<ScoredCandidate> ranked = scoreCalculator.rank(List.of(
			candidate(1, 3000, 500, 5, 3)
		), RADIUS_M, TODAY);

		assertThat(ranked).hasSize(1);
		assertThat(ranked.get(0).breakdown().priceScore()).isEqualTo(1.0);
	}

	@Test
	void 모든_후보_가격이_같으면_전원_priceScore가_1이다() {
		List<ScoredCandidate> ranked = scoreCalculator.rank(List.of(
			candidate(1, 3000, 500, 5, 3),
			candidate(2, 3000, 1000, 5, 3),
			candidate(3, 3000, 1500, 5, 3)
		), RADIUS_M, TODAY);

		assertThat(ranked).allSatisfy(sc -> assertThat(sc.breakdown().priceScore()).isEqualTo(1.0));
	}

	@Test
	void 동일_입력_2회_호출시_순서가_완전히_일치한다() {
		List<Candidate> candidates = List.of(
			candidate(1, 3000, 500, 5, 3),
			candidate(2, 2800, 1200, 10, 2),
			candidate(3, 3100, 300, 2, 1)
		);

		List<ScoredCandidate> first = scoreCalculator.rank(candidates, RADIUS_M, TODAY);
		List<ScoredCandidate> second = scoreCalculator.rank(candidates, RADIUS_M, TODAY);

		List<Long> firstOrder = first.stream().map(sc -> sc.candidate().pharmacyId()).toList();
		List<Long> secondOrder = second.stream().map(sc -> sc.candidate().pharmacyId()).toList();
		assertThat(firstOrder).isEqualTo(secondOrder);
	}

	@Test
	void 가장_싼_약국이_반경_경계이고_2위가_코앞이면_거리_가중치로_순위가_역전된다() {
		// 1번: 가장 싸지만 반경 끝(distanceScore=0), 2번: 가격은 비싸지만 바로 코앞(distanceScore=1)
		List<ScoredCandidate> ranked = scoreCalculator.rank(List.of(
			candidate(1, 2000, RADIUS_M, 5, 3),
			candidate(2, 2900, 0, 5, 3)
		), RADIUS_M, TODAY);

		// priceScore: 1번=1.0, 2번=0.0 / distanceScore: 1번=0.0, 2번=1.0
		// score_1 = 0.60*1.0 + 0.25*0.0 = 0.60, score_2 = 0.60*0.0 + 0.25*1.0 = 0.25 (freshness는 동일)
		assertThat(ranked.get(0).candidate().pharmacyId()).isEqualTo(1);
	}

	@Test
	void reportCount가_1이고_ageDays가_40이면_LOW_CONFIDENCE와_STALE_DATA_뱃지가_붙는다() {
		List<ScoredCandidate> ranked = scoreCalculator.rank(List.of(
			candidate(1, 3000, 500, 40, 1)
		), RADIUS_M, TODAY);

		assertThat(ranked.get(0).badges()).contains(Badge.LOW_CONFIDENCE, Badge.STALE_DATA);
	}
}
