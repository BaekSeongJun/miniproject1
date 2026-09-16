package com.pharmaprice.report.service;

import com.pharmaprice.auth.domain.AppUser;
import com.pharmaprice.auth.repository.AppUserRepository;
import com.pharmaprice.common.dto.PageResponse;
import com.pharmaprice.common.exception.ApiException;
import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.drug.repository.DrugRepository;
import com.pharmaprice.pharmacy.domain.Pharmacy;
import com.pharmaprice.pharmacy.repository.PharmacyRepository;
import com.pharmaprice.recommendation.domain.PharmacyDrugPriceStat;
import com.pharmaprice.recommendation.service.PriceStatService;
import com.pharmaprice.report.domain.FlagReason;
import com.pharmaprice.report.domain.PriceReport;
import com.pharmaprice.report.domain.ReportSource;
import com.pharmaprice.report.domain.ReportStatus;
import com.pharmaprice.report.domain.UploadedFile;
import com.pharmaprice.report.dto.PriceReportCreateRequest;
import com.pharmaprice.report.dto.PriceReportListItemResponse;
import com.pharmaprice.report.dto.PriceReportResponse;
import com.pharmaprice.report.repository.PriceReportQueryRepository;
import com.pharmaprice.report.repository.PriceReportRepository;
import com.pharmaprice.report.repository.UploadedFileRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PriceReportService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int MAX_PAST_DAYS = 180;
	private static final BigDecimal OUTLIER_LOW_RATIO = new BigDecimal("0.3");
	private static final BigDecimal OUTLIER_HIGH_RATIO = new BigDecimal("3");
	private static final int MAX_PAGE_SIZE = 50;

	private final PriceReportRepository priceReportRepository;
	private final PriceReportQueryRepository priceReportQueryRepository;
	private final PharmacyRepository pharmacyRepository;
	private final DrugRepository drugRepository;
	private final AppUserRepository appUserRepository;
	private final UploadedFileRepository uploadedFileRepository;
	private final PriceStatService priceStatService;

	public PriceReportResponse create(long userId, PriceReportCreateRequest request) {
		Pharmacy pharmacy = pharmacyRepository.findById(request.pharmacyId())
			.orElseThrow(() -> new NoSuchElementException("PHARMACY_NOT_FOUND"));
		Drug drug = drugRepository.findById(request.drugId())
			.orElseThrow(() -> new NoSuchElementException("DRUG_NOT_FOUND"));
		if (!Boolean.TRUE.equals(drug.getOtcFlag())) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DRUG_NOT_OTC", "전문의약품은 제보할 수 없습니다.");
		}

		LocalDate purchasedAt = request.purchasedAt() != null ? request.purchasedAt() : LocalDate.now(KST);
		LocalDate today = LocalDate.now(KST);
		if (purchasedAt.isAfter(today) || purchasedAt.isBefore(today.minusDays(MAX_PAST_DAYS))) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "구매일은 오늘 이전 180일 이내여야 합니다.");
		}

		AppUser user = appUserRepository.getReferenceById(userId);
		UploadedFile receiptFile = request.receiptFileId() != null
			? uploadedFileRepository.findById(request.receiptFileId())
				.orElseThrow(() -> new NoSuchElementException("UPLOADED_FILE_NOT_FOUND"))
			: null;

		FlagReason flagReason = detectOutlier(request.drugId(), request.price());
		boolean flagged = flagReason != null;

		PriceReport report = PriceReport.builder()
			.pharmacy(pharmacy)
			.drug(drug)
			.user(user)
			.price(request.price())
			.purchasedAt(purchasedAt)
			.source(ReportSource.FORM)
			.status(ReportStatus.ACTIVE)
			.flagged(flagged)
			.flagReason(flagReason)
			.receiptFile(receiptFile)
			.memo(request.memo())
			.build();

		try {
			report = priceReportRepository.save(report);
			priceReportRepository.flush();
		} catch (DataIntegrityViolationException e) {
			throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_REPORT", "같은 약국·약품에 대해 오늘 이미 제보했습니다.");
		}

		Optional<PharmacyDrugPriceStat> stat = priceStatService.recalculate(request.pharmacyId(), request.drugId());
		user.increaseReportCount();

		String warning = flagged
			? "입력하신 가격이 이 약품의 일반적인 가격대와 크게 달라 통계에 반영되지 않았습니다. 관리자 확인 후 반영됩니다."
			: null;
		return PriceReportResponse.of(report, warning, stat.map(this::toUpdatedStat).orElse(null));
	}

	@Transactional(readOnly = true)
	public PageResponse<PriceReportListItemResponse> list(
			Long pharmacyId, Long drugId, boolean mine, Long currentUserId, int page, int size) {
		if (mine && currentUserId == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "본인 제보 조회는 로그인이 필요합니다.");
		}
		Long userId = mine ? currentUserId : null;
		Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
		Page<PriceReportQueryRepository.PriceReportListProjection> result =
			priceReportQueryRepository.search(pharmacyId, drugId, userId, pageable);
		return PageResponse.of(result.map(PriceReportListItemResponse::from));
	}

	private FlagReason detectOutlier(long drugId, int price) {
		BigDecimal median = priceReportRepository.findDrugMedianPrice(drugId).orElse(null);
		if (median == null) {
			return null;
		}
		BigDecimal p = BigDecimal.valueOf(price);
		if (p.compareTo(median.multiply(OUTLIER_LOW_RATIO)) < 0) {
			return FlagReason.OUTLIER_LOW;
		}
		if (p.compareTo(median.multiply(OUTLIER_HIGH_RATIO)) > 0) {
			return FlagReason.OUTLIER_HIGH;
		}
		return null;
	}

	private PriceReportResponse.UpdatedStat toUpdatedStat(PharmacyDrugPriceStat stat) {
		return new PriceReportResponse.UpdatedStat(
			stat.getRepPrice(), stat.getMinPrice(), stat.getAvgPrice(), stat.getReportCount(), stat.getLastReportedAt());
	}
}
