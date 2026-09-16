package com.pharmaprice.drug.controller;

import com.pharmaprice.common.dto.PageResponse;
import com.pharmaprice.drug.dto.DrugDetailResponse;
import com.pharmaprice.drug.dto.DrugSummaryResponse;
import com.pharmaprice.drug.service.DrugService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drugs")
@RequiredArgsConstructor
public class DrugController {

	private final DrugService drugService;

	@GetMapping
	public PageResponse<DrugSummaryResponse> search(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String category,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return drugService.search(q, category, page, size);
	}

	@GetMapping("/{drugId}")
	public DrugDetailResponse findDetail(@PathVariable long drugId) {
		return drugService.findDetail(drugId);
	}
}
