package com.pharmaprice.admin.service;

import com.pharmaprice.common.dto.PageResponse;
import com.pharmaprice.recommendation.domain.PharmacyDrugPriceStat;
import com.pharmaprice.recommendation.service.PriceStatService;
import com.pharmaprice.report.domain.PriceReport;
import com.pharmaprice.report.domain.ReportStatus;
import com.pharmaprice.report.dto.AdminPriceReportListItemResponse;
import com.pharmaprice.report.dto.AdminPriceReportUpdateRequest;
import com.pharmaprice.report.dto.AdminPriceReportUpdateResponse;
import com.pharmaprice.report.dto.AdminPriceReportUpdateResponse.RecalculatedStat;
import com.pharmaprice.report.repository.PriceReportQueryRepository;
import com.pharmaprice.report.repository.PriceReportRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminReportService {

	private static final int MAX_PAGE_SIZE = 50;

	private final PriceReportRepository priceReportRepository;
	private final PriceReportQueryRepository priceReportQueryRepository;
	private final PriceStatService priceStatService;

	@Transactional(readOnly = true)
	public PageResponse<AdminPriceReportListItemResponse> list(
			Boolean flagged, String status, Long pharmacyId, Long drugId, String keyword, int page, int size) {
		Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
		String trimmedKeyword = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
		var result = priceReportQueryRepository.searchForAdmin(
			flagged, status, pharmacyId, drugId, trimmedKeyword, pageable);
		return PageResponse.of(result.map(AdminPriceReportListItemResponse::from));
	}

	public AdminPriceReportUpdateResponse update(long reportId, AdminPriceReportUpdateRequest request) {
		PriceReport report = priceReportRepository.findById(reportId)
			.orElseThrow(() -> new NoSuchElementException("PRICE_REPORT_NOT_FOUND"));

		ReportStatus previousStatus = report.getStatus();
		if (request.status() != null) {
			report.changeStatus(request.status());
		}
		if (request.flagged() != null) {
			report.changeFlagged(request.flagged());
		}
		if (request.reason() != null) {
			report.changeFlagReason(request.reason());
		}

		// REJECTED로 전환되는 경우에만 제보자의 report_count를 차감한다 (이미 REJECTED였다면 중복 차감하지 않는다)
		if (request.status() == ReportStatus.REJECTED && previousStatus != ReportStatus.REJECTED
				&& report.getUser() != null) {
			report.getUser().decreaseReportCount();
		}

		RecalculatedStat recalculatedStat = null;
		if (request.status() != null || request.flagged() != null) {
			Optional<PharmacyDrugPriceStat> stat = priceStatService.recalculate(
				report.getPharmacy().getId(), report.getDrug().getId());
			recalculatedStat = stat
				.map(s -> new RecalculatedStat(
					s.getPharmacy().getId(), s.getDrug().getId(), s.getRepPrice(), s.getReportCount()))
				.orElse(new RecalculatedStat(report.getPharmacy().getId(), report.getDrug().getId(), null, 0));
		}

		return AdminPriceReportUpdateResponse.of(report, recalculatedStat);
	}
}
