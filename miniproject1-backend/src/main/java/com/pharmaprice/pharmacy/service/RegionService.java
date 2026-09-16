package com.pharmaprice.pharmacy.service;

import com.pharmaprice.pharmacy.dto.RegionResponse;
import com.pharmaprice.pharmacy.repository.RegionQueryRepository;
import com.pharmaprice.pharmacy.repository.RegionQueryRepository.RegionProjection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

	private final RegionQueryRepository regionQueryRepository;

	public List<RegionResponse> findAllGroupedBySido() {
		Map<String, List<RegionResponse.Sigungu>> grouped = new LinkedHashMap<>();
		for (RegionProjection p : regionQueryRepository.findAllWithPharmacyCount()) {
			grouped.computeIfAbsent(p.getSido(), k -> new ArrayList<>())
				.add(new RegionResponse.Sigungu(p.getCode(), p.getSigungu(), p.getCenterLat(), p.getCenterLng(), p.getPharmacyCount()));
		}
		return grouped.entrySet().stream()
			.map(e -> new RegionResponse(e.getKey(), e.getValue()))
			.toList();
	}
}
