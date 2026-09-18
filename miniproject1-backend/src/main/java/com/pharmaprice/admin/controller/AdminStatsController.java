package com.pharmaprice.admin.controller;

import com.pharmaprice.admin.dto.AdminStatsResponse.DrugStats;
import com.pharmaprice.admin.dto.AdminStatsResponse.Overview;
import com.pharmaprice.admin.dto.AdminStatsResponse.PriceGaps;
import com.pharmaprice.admin.dto.AdminStatsResponse.RegionStats;
import com.pharmaprice.admin.dto.AdminStatsResponse.TopDrugsByReports;
import com.pharmaprice.admin.dto.AdminStatsResponse.TopDrugsBySearches;
import com.pharmaprice.admin.dto.AdminStatsResponse.TopPharmacies;
import com.pharmaprice.admin.dto.AdminStatsResponse.TopReporters;
import com.pharmaprice.admin.service.AdminStatsService;
import com.pharmaprice.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

	private final AdminStatsService adminStatsService;

	@GetMapping("/overview")
	public Overview overview() {
		return adminStatsService.overview();
	}

	@GetMapping("/regions")
	public PageResponse<RegionStats.Row> regions(
			@RequestParam(required = false) String regionCode,
			@RequestParam(required = false) Long drugId,
			@RequestParam(required = false) String sido,
			@RequestParam(defaultValue = "reportCount") String sortCol,
			@RequestParam(defaultValue = "DESC") String sortDir,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return adminStatsService.regions(regionCode, drugId, sido, sortCol, sortDir, page, size);
	}

	@GetMapping("/drugs/{drugId}")
	public DrugStats drugStats(@PathVariable long drugId) {
		return adminStatsService.drugStats(drugId);
	}

	@GetMapping("/price-gaps")
	public PriceGaps priceGaps(@RequestParam(defaultValue = "10") int limit) {
		return adminStatsService.priceGaps(limit);
	}

	@GetMapping("/top-pharmacies")
	public TopPharmacies topPharmacies(
			@RequestParam(defaultValue = "ALL") String period,
			@RequestParam(defaultValue = "10") int limit) {
		return adminStatsService.topPharmacies(period, limit);
	}

	@GetMapping("/top-reporters")
	public TopReporters topReporters(
			@RequestParam(defaultValue = "ALL") String period,
			@RequestParam(defaultValue = "10") int limit) {
		return adminStatsService.topReporters(period, limit);
	}

	@GetMapping("/top-drugs-by-reports")
	public TopDrugsByReports topDrugsByReports(
			@RequestParam(defaultValue = "ALL") String period,
			@RequestParam(defaultValue = "10") int limit) {
		return adminStatsService.topDrugsByReports(period, limit);
	}

	@GetMapping("/top-drugs-by-searches")
	public TopDrugsBySearches topDrugsBySearches(
			@RequestParam(defaultValue = "ALL") String period,
			@RequestParam(defaultValue = "10") int limit) {
		return adminStatsService.topDrugsBySearches(period, limit);
	}
}
