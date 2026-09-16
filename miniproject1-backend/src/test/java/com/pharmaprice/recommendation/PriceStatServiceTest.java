package com.pharmaprice.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmaprice.AbstractIntegrationTest;
import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.drug.repository.DrugRepository;
import com.pharmaprice.pharmacy.domain.Pharmacy;
import com.pharmaprice.pharmacy.domain.Region;
import com.pharmaprice.pharmacy.repository.PharmacyRepository;
import com.pharmaprice.pharmacy.repository.RegionRepository;
import com.pharmaprice.recommendation.domain.PharmacyDrugPriceStat;
import com.pharmaprice.recommendation.repository.PriceStatRepository;
import com.pharmaprice.recommendation.service.PriceStatService;
import com.pharmaprice.report.domain.PriceReport;
import com.pharmaprice.report.domain.ReportSource;
import com.pharmaprice.report.domain.ReportStatus;
import com.pharmaprice.report.repository.PriceReportRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PriceStatServiceTest extends AbstractIntegrationTest {

	@Autowired
	private PriceStatService priceStatService;
	@Autowired
	private PriceStatRepository priceStatRepository;
	@Autowired
	private PriceReportRepository priceReportRepository;
	@Autowired
	private PharmacyRepository pharmacyRepository;
	@Autowired
	private DrugRepository drugRepository;
	@Autowired
	private RegionRepository regionRepository;

	private Pharmacy pharmacy;
	private Drug drug;

	@BeforeEach
	void setUp() {
		Region region = regionRepository.save(Region.builder()
			.code("11680").sido("서울특별시").sigungu("강남구").centerLat(37.4979).centerLng(127.0276).build());
		pharmacy = pharmacyRepository.save(Pharmacy.builder()
			.name("행복약국").region(region).lat(37.4979).lng(127.0276).isActive(true).build());
		drug = drugRepository.save(Drug.builder()
			.name("타이레놀정500밀리그람").displayName("타이레놀 500mg").category("해열진통")
			.packageUnit("8정").otcFlag(true).build());
	}

	private void report(int price, int daysAgo) {
		priceReportRepository.save(PriceReport.builder()
			.pharmacy(pharmacy).drug(drug).price(price)
			.purchasedAt(LocalDate.now().minusDays(daysAgo))
			.source(ReportSource.FORM).status(ReportStatus.ACTIVE).flagged(false)
			.build());
	}

	@Test
	void 극단값이_섞여도_중앙값은_흔들리지_않는다() {
		report(3000, 1);
		report(3100, 2);
		report(3200, 3);
		report(3050, 4);
		report(50000, 5); // 극단값

		Optional<PharmacyDrugPriceStat> result = priceStatService.recalculate(pharmacy.getId(), drug.getId());

		// 극단값(50000) 제거 후 남는 4건 [3000, 3050, 3100, 3200]의 중앙값(선형보간): (3050+3100)/2
		assertThat(result).isPresent();
		assertThat(result.get().getRepPrice()).isEqualTo(3075);
	}

	@Test
	void 표본이_3건이면_IQR_제거_없이_중앙값을_반환한다() {
		report(3000, 1);
		report(9000, 2); // 3건뿐이라 IQR 제거 대상이어도 그대로 남아야 함
		report(3200, 3);

		Optional<PharmacyDrugPriceStat> result = priceStatService.recalculate(pharmacy.getId(), drug.getId());

		assertThat(result).isPresent();
		assertThat(result.get().getReportCount()).isEqualTo(3);
		assertThat(result.get().getRepPrice()).isEqualTo(3200);
	}

	@Test
	void 최근_90일_내_제보가_없으면_180일_창으로_확대된다() {
		report(3000, 100);
		report(3100, 110);

		Optional<PharmacyDrugPriceStat> result = priceStatService.recalculate(pharmacy.getId(), drug.getId());

		assertThat(result).isPresent();
		assertThat(result.get().getWindowDays()).isEqualTo((short) 180);
	}

	@Test
	void 유효_제보가_없으면_stat_행이_삭제된다() {
		priceStatService.recalculate(pharmacy.getId(), drug.getId());

		Optional<PharmacyDrugPriceStat> result = priceStatRepository.findByPharmacyIdAndDrugId(pharmacy.getId(), drug.getId());

		assertThat(result).isEmpty();
	}

	@Test
	void 같은_조합에_두번_호출해도_결과가_동일하다() {
		report(3000, 1);
		report(3200, 2);

		Optional<PharmacyDrugPriceStat> first = priceStatService.recalculate(pharmacy.getId(), drug.getId());
		Optional<PharmacyDrugPriceStat> second = priceStatService.recalculate(pharmacy.getId(), drug.getId());

		assertThat(first.get().getRepPrice()).isEqualTo(second.get().getRepPrice());
		assertThat(first.get().getReportCount()).isEqualTo(second.get().getReportCount());
	}
}
