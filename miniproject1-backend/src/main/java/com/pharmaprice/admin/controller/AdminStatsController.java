package com.pharmaprice.admin.controller;

import com.pharmaprice.admin.dto.AdminStatsResponse.DrugStats;
import com.pharmaprice.admin.dto.AdminStatsResponse.Overview;
import com.pharmaprice.admin.dto.AdminStatsResponse.PriceGaps;
import com.pharmaprice.admin.dto.AdminStatsResponse.RegionStats;
import com.pharmaprice.admin.service.AdminStatsService;
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
	public RegionStats regions(
			@RequestParam(required = false) String regionCode,
			@RequestParam(required = false) Long drugId,
			@RequestParam(required = false) String sido) {
		return adminStatsService.regions(regionCode, drugId, sido);
	}

	@GetMapping("/drugs/{drugId}")
	public DrugStats drugStats(@PathVariable long drugId) {
		return adminStatsService.drugStats(drugId);
	}

	@GetMapping("/price-gaps")
	public PriceGaps priceGaps(@RequestParam(defaultValue = "10") int limit) {
		return adminStatsService.priceGaps(limit);
	}
}
