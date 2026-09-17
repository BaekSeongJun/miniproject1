package com.pharmaprice.admin.controller;

import com.pharmaprice.admin.service.AdminReportService;
import com.pharmaprice.common.dto.PageResponse;
import com.pharmaprice.report.dto.AdminPriceReportListItemResponse;
import com.pharmaprice.report.dto.AdminPriceReportUpdateRequest;
import com.pharmaprice.report.dto.AdminPriceReportUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/price-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

	private final AdminReportService adminReportService;

	@GetMapping
	public PageResponse<AdminPriceReportListItemResponse> list(
			@RequestParam(required = false) Boolean flagged,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long pharmacyId,
			@RequestParam(required = false) Long drugId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return adminReportService.list(flagged, status, pharmacyId, drugId, page, size);
	}

	@PatchMapping("/{reportId}")
	public AdminPriceReportUpdateResponse update(
			@PathVariable long reportId, @RequestBody AdminPriceReportUpdateRequest request) {
		return adminReportService.update(reportId, request);
	}
}
