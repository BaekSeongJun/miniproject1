package com.pharmaprice.report.controller;

import com.pharmaprice.auth.security.UserPrincipal;
import com.pharmaprice.common.dto.PageResponse;
import com.pharmaprice.report.dto.PriceReportCreateRequest;
import com.pharmaprice.report.dto.PriceReportListItemResponse;
import com.pharmaprice.report.dto.PriceReportResponse;
import com.pharmaprice.report.service.PriceReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/price-reports")
@RequiredArgsConstructor
public class PriceReportController {

	private final PriceReportService priceReportService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PriceReportResponse create(
			@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody PriceReportCreateRequest request) {
		return priceReportService.create(principal.userId(), request);
	}

	@GetMapping
	public PageResponse<PriceReportListItemResponse> list(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) Long pharmacyId,
			@RequestParam(required = false) Long drugId,
			@RequestParam(defaultValue = "false") boolean mine,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Long currentUserId = principal != null ? principal.userId() : null;
		return priceReportService.list(pharmacyId, drugId, mine, currentUserId, page, size);
	}
}
