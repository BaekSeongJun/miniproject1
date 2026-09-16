package com.pharmaprice.recommendation.dto;

import java.time.LocalDate;

public record Candidate(
	long pharmacyId, int repPrice, double distanceM,
	LocalDate lastReportedAt, int reportCount
) {}
