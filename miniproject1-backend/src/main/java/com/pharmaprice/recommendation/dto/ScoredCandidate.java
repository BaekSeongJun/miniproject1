package com.pharmaprice.recommendation.dto;

import java.util.List;

public record ScoredCandidate(
	Candidate candidate, double score, ScoreBreakdown breakdown, List<Badge> badges
) {}
