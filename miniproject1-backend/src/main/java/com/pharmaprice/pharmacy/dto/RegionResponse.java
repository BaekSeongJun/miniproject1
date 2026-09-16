package com.pharmaprice.pharmacy.dto;

import java.util.List;

public record RegionResponse(String sido, List<Sigungu> sigungus) {

	public record Sigungu(String code, String sigungu, Double centerLat, Double centerLng, Long pharmacyCount) {}
}
