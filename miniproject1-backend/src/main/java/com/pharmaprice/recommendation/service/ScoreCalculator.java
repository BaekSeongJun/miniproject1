package com.pharmaprice.recommendation.service;

import com.pharmaprice.recommendation.dto.Candidate;
import com.pharmaprice.recommendation.dto.ScoredCandidate;
import java.time.LocalDate;
import java.util.List;

public interface ScoreCalculator {

	/** 후보군의 가격·거리·신선도 Score를 계산해 순위대로 정렬한 목록을 반환한다. */
	List<ScoredCandidate> rank(List<Candidate> candidates, int radiusM, LocalDate today);
}
