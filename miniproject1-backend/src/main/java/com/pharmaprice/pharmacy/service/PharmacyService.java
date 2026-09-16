package com.pharmaprice.pharmacy.service;

import com.pharmaprice.common.dto.PageResponse;
import com.pharmaprice.pharmacy.domain.Pharmacy;
import com.pharmaprice.pharmacy.domain.Region;
import com.pharmaprice.pharmacy.dto.DrugPriceHistoryResponse;
import com.pharmaprice.pharmacy.dto.PharmacyDetailResponse;
import com.pharmaprice.pharmacy.dto.PharmacySummaryResponse;
import com.pharmaprice.pharmacy.repository.PharmacyQueryRepository;
import com.pharmaprice.pharmacy.repository.PharmacyQueryRepository.DrugPriceProjection;
import com.pharmaprice.pharmacy.repository.PharmacyQueryRepository.PharmacySummaryProjection;
import com.pharmaprice.pharmacy.repository.PharmacyQueryRepository.PriceHistoryProjection;
import com.pharmaprice.recommendation.distance.BoundingBox;
import com.pharmaprice.recommendation.distance.CoordinateValidator;
import com.pharmaprice.recommendation.distance.DistanceCalculator;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PharmacyService {

	private static final int DEFAULT_RADIUS_M = 2000;
	private static final int MAX_RADIUS_M = 10000;
	private static final int MAX_PAGE_SIZE = 50;
	private static final int DEFAULT_HISTORY_DAYS = 180;
	private static final int MAX_HISTORY_DAYS = 365;

	private final PharmacyQueryRepository pharmacyQueryRepository;
	private final DistanceCalculator distanceCalculator;

	public PageResponse<PharmacySummaryResponse> search(String q, Double lat, Double lng, Integer radius,
			int page, int size) {
		boolean hasQ = q != null && !q.isBlank();
		boolean hasCoord = lat != null && lng != null;
		if (!hasQ && !hasCoord) {
			throw new IllegalArgumentException("VALIDATION_FAILED: q or lat+lng is required");
		}
		if (hasCoord && !CoordinateValidator.isValid(lat, lng)) {
			throw new IllegalArgumentException("INVALID_COORDINATE: lat=" + lat + ", lng=" + lng);
		}

		int radiusM = Math.min(radius == null ? DEFAULT_RADIUS_M : radius, MAX_RADIUS_M);
		BoundingBox box = hasCoord ? distanceCalculator.boundingBox(lat, lng, radiusM) : null;

		Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
		Page<PharmacySummaryProjection> result = pharmacyQueryRepository.search(
			hasQ ? q : null,
			box == null ? null : box.minLat(), box == null ? null : box.maxLat(),
			box == null ? null : box.minLng(), box == null ? null : box.maxLng(),
			pageable);

		return PageResponse.of(result.map(p -> toSummaryResponse(p, hasCoord, lat, lng)));
	}

	public PharmacyDetailResponse findDetail(long pharmacyId, Double lat, Double lng) {
		boolean hasCoord = lat != null && lng != null;
		if (hasCoord && !CoordinateValidator.isValid(lat, lng)) {
			throw new IllegalArgumentException("INVALID_COORDINATE: lat=" + lat + ", lng=" + lng);
		}

		Pharmacy pharmacy = pharmacyQueryRepository.findActiveDetailById(pharmacyId)
			.orElseThrow(() -> new NoSuchElementException("PHARMACY_NOT_FOUND"));

		Long distanceM = hasCoord
			? Math.round(distanceCalculator.distanceMeters(lat, lng, pharmacy.getLat(), pharmacy.getLng()))
			: null;

		List<PharmacyDetailResponse.DrugPrice> drugPrices = pharmacyQueryRepository
			.findDrugPricesByPharmacyId(pharmacyId).stream()
			.map(this::toDrugPrice)
			.toList();

		Region region = pharmacy.getRegion();
		PharmacySummaryResponse.Region regionDto = region == null
			? null
			: new PharmacySummaryResponse.Region(region.getCode(), region.getSido(), region.getSigungu());

		return new PharmacyDetailResponse(
			pharmacy.getId(), pharmacy.getName(), pharmacy.getAddressRoad(), pharmacy.getAddressJibun(),
			pharmacy.getLat(), pharmacy.getLng(), pharmacy.getPhone(), pharmacy.getBusinessHours(),
			distanceM, regionDto, drugPrices);
	}

	public DrugPriceHistoryResponse findHistory(long pharmacyId, long drugId, Integer days) {
		int effDays = Math.min(days == null ? DEFAULT_HISTORY_DAYS : days, MAX_HISTORY_DAYS);
		LocalDate fromDate = LocalDate.now().minusDays(effDays);

		List<DrugPriceHistoryResponse.Point> points = pharmacyQueryRepository
			.findHistory(pharmacyId, drugId, fromDate).stream()
			.map(this::toPoint)
			.toList();

		return new DrugPriceHistoryResponse(pharmacyId, drugId, points);
	}

	private PharmacySummaryResponse toSummaryResponse(PharmacySummaryProjection p, boolean hasCoord, Double lat, Double lng) {
		Long distanceM = hasCoord
			? Math.round(distanceCalculator.distanceMeters(lat, lng, p.getLat(), p.getLng()))
			: null;
		PharmacySummaryResponse.Region region = p.getRegionCode() == null
			? null
			: new PharmacySummaryResponse.Region(p.getRegionCode(), p.getRegionSido(), p.getRegionSigungu());
		return new PharmacySummaryResponse(
			p.getId(), p.getName(), p.getAddressRoad(), p.getLat(), p.getLng(), p.getPhone(), distanceM, region);
	}

	private PharmacyDetailResponse.DrugPrice toDrugPrice(DrugPriceProjection p) {
		return new PharmacyDetailResponse.DrugPrice(
			p.getDrugId(), p.getDisplayName(), p.getPackageUnit(),
			p.getRepPrice(), p.getMinPrice(), p.getMaxPrice(), p.getAvgPrice(),
			p.getReportCount(), p.getLastReportedAt(),
			p.getNationalAvgPrice(), p.getRepPrice() - p.getNationalAvgPrice());
	}

	private DrugPriceHistoryResponse.Point toPoint(PriceHistoryProjection p) {
		return new DrugPriceHistoryResponse.Point(p.getPurchasedAt(), p.getPrice(), p.getFlagged());
	}
}
