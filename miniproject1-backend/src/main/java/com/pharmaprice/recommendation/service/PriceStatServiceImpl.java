package com.pharmaprice.recommendation.service;

import com.pharmaprice.common.config.RecommendationProperties;
import com.pharmaprice.recommendation.domain.PharmacyDrugPriceStat;
import com.pharmaprice.recommendation.repository.PriceStatRepository;
import com.pharmaprice.recommendation.repository.PriceStatRepository.RecalculatedStat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PriceStatServiceImpl implements PriceStatService {

	private final PriceStatRepository priceStatRepository;
	private final RecommendationProperties recommendationProperties;

	@Override
	public Optional<PharmacyDrugPriceStat> recalculate(long pharmacyId, long drugId) {
		int primaryWindow = recommendationProperties.priceWindowDays();
		int fallbackWindow = recommendationProperties.priceWindowFallbackDays();

		RecalculatedStat stat = priceStatRepository.calculateStat(pharmacyId, drugId, primaryWindow)
			.orElseThrow(() -> new IllegalStateException("통계 쿼리가 결과를 반환하지 않았습니다."));
		int windowDays = primaryWindow;

		if (stat.getReportCount() == 0) {
			stat = priceStatRepository.calculateStat(pharmacyId, drugId, fallbackWindow)
				.orElseThrow(() -> new IllegalStateException("통계 쿼리가 결과를 반환하지 않았습니다."));
			windowDays = fallbackWindow;
		}

		if (stat.getReportCount() == 0) {
			priceStatRepository.deleteByPharmacyIdAndDrugId(pharmacyId, drugId);
			return Optional.empty();
		}

		priceStatRepository.upsert(
			pharmacyId, drugId,
			stat.getRepPrice(), stat.getMinPrice(), stat.getMaxPrice(), stat.getAvgPrice(),
			stat.getReportCount(), stat.getLastReportedAt(), (short) windowDays);

		return priceStatRepository.findByPharmacyIdAndDrugId(pharmacyId, drugId);
	}
}
