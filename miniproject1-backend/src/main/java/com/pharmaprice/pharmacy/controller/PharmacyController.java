package com.pharmaprice.pharmacy.controller;

import com.pharmaprice.common.dto.PageResponse;
import com.pharmaprice.pharmacy.dto.DrugPriceHistoryResponse;
import com.pharmaprice.pharmacy.dto.PharmacyDetailResponse;
import com.pharmaprice.pharmacy.dto.PharmacySummaryResponse;
import com.pharmaprice.pharmacy.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pharmacies")
@RequiredArgsConstructor
public class PharmacyController {

	private final PharmacyService pharmacyService;

	@GetMapping
	public PageResponse<PharmacySummaryResponse> search(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Double lat,
			@RequestParam(required = false) Double lng,
			@RequestParam(required = false) Integer radius,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return pharmacyService.search(q, lat, lng, radius, page, size);
	}

	@GetMapping("/{pharmacyId}")
	public PharmacyDetailResponse findDetail(
			@PathVariable long pharmacyId,
			@RequestParam(required = false) Double lat,
			@RequestParam(required = false) Double lng) {
		return pharmacyService.findDetail(pharmacyId, lat, lng);
	}

	@GetMapping("/{pharmacyId}/drugs/{drugId}/history")
	public DrugPriceHistoryResponse findHistory(
			@PathVariable long pharmacyId,
			@PathVariable long drugId,
			@RequestParam(required = false) Integer days) {
		return pharmacyService.findHistory(pharmacyId, drugId, days);
	}
}
