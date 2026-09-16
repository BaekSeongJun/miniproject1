package com.pharmaprice.recommendation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaprice.AbstractIntegrationTest;
import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.drug.repository.DrugRepository;
import com.pharmaprice.pharmacy.domain.Pharmacy;
import com.pharmaprice.pharmacy.domain.Region;
import com.pharmaprice.pharmacy.repository.PharmacyRepository;
import com.pharmaprice.pharmacy.repository.RegionRepository;
import com.pharmaprice.recommendation.domain.PharmacyDrugPriceStat;
import com.pharmaprice.recommendation.repository.PriceStatRepository;
import com.pharmaprice.report.domain.PriceReport;
import com.pharmaprice.report.domain.ReportSource;
import com.pharmaprice.report.domain.ReportStatus;
import com.pharmaprice.report.repository.PriceReportRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SearchControllerIntegrationTest extends AbstractIntegrationTest {

	// 강남역 부근 좌표
	private static final double BASE_LAT = 37.4979;
	private static final double BASE_LNG = 127.0276;

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private DrugRepository drugRepository;
	@Autowired
	private RegionRepository regionRepository;
	@Autowired
	private PharmacyRepository pharmacyRepository;
	@Autowired
	private PriceStatRepository priceStatRepository;
	@Autowired
	private PriceReportRepository priceReportRepository;

	private Drug drug;
	private Region region;

	@BeforeEach
	void setUp() {
		drug = drugRepository.save(Drug.builder()
			.itemSeq("196800050").name("타이레놀정500밀리그람").displayName("타이레놀 500mg")
			.maker("한국얀센").category("해열진통").form("정제").packageUnit("8정")
			.otcFlag(true).basePrice(3000).build());
		region = regionRepository.save(Region.builder()
			.code("11680").sido("서울특별시").sigungu("강남구").centerLat(BASE_LAT).centerLng(BASE_LNG).build());
	}

	private Pharmacy pharmacy(String name, double lat, double lng) {
		return pharmacyRepository.save(Pharmacy.builder()
			.name(name).addressRoad("서울시 강남구 " + name).region(region)
			.lat(lat).lng(lng).isActive(true).build());
	}

	private void stat(Pharmacy pharmacy, int repPrice, int minPrice, int maxPrice, int avgPrice,
			int reportCount, LocalDate lastReportedAt) {
		priceStatRepository.save(PharmacyDrugPriceStat.builder()
			.pharmacy(pharmacy).drug(drug)
			.repPrice(repPrice).minPrice(minPrice).maxPrice(maxPrice).avgPrice(avgPrice)
			.reportCount(reportCount).lastReportedAt(lastReportedAt).windowDays((short) 90)
			.calculatedAt(Instant.now()).build());
	}

	private void report(Pharmacy pharmacy, ReportSource source) {
		priceReportRepository.save(PriceReport.builder()
			.pharmacy(pharmacy).drug(drug).price(3000).purchasedAt(LocalDate.now())
			.source(source).status(ReportStatus.ACTIVE).flagged(false).build());
	}

	@Test
	void 정상_검색_응답_구조가_스펙과_일치한다() throws Exception {
		Pharmacy near = pharmacy("가온약국", BASE_LAT + 0.001, BASE_LNG);
		stat(near, 2600, 2500, 2700, 2640, 4, LocalDate.now().minusDays(5));
		report(near, ReportSource.SEED);

		mockMvc.perform(get("/api/v1/search")
				.param("drugId", String.valueOf(drug.getId()))
				.param("lat", String.valueOf(BASE_LAT))
				.param("lng", String.valueOf(BASE_LNG)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.drug.displayName").value("타이레놀 500mg"))
			.andExpect(jsonPath("$.query.locationSource").value("GPS"))
			.andExpect(jsonPath("$.summary.resultCount").value(1))
			.andExpect(jsonPath("$.dataSource").value("SEED"))
			.andExpect(jsonPath("$.results[0].rank").value(1))
			.andExpect(jsonPath("$.results[0].recommended").value(true))
			.andExpect(jsonPath("$.results[0].pharmacy.name").value("가온약국"))
			.andExpect(jsonPath("$.results[0].price.repPrice").value(2600))
			.andExpect(jsonPath("$.results[0].scoreBreakdown.weights.price").value(0.6))
			.andExpect(jsonPath("$.results[0].badges").isArray());
	}

	@Test
	void 결과_0건이면_반경_확대시_예상_건수를_계산해_내려준다() throws Exception {
		// 500m 밖(약 600m), 1000m 안에 위치
		Pharmacy far = pharmacy("먼약국", BASE_LAT + 0.0054, BASE_LNG);
		stat(far, 3000, 3000, 3000, 3000, 1, LocalDate.now());

		mockMvc.perform(get("/api/v1/search")
				.param("drugId", String.valueOf(drug.getId()))
				.param("lat", String.valueOf(BASE_LAT))
				.param("lng", String.valueOf(BASE_LNG))
				.param("radius", "500"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.summary.resultCount").value(0))
			.andExpect(jsonPath("$.results").isEmpty())
			.andExpect(jsonPath("$.suggestion.type").value("EXPAND_RADIUS"))
			.andExpect(jsonPath("$.suggestion.recommendedRadius").value(1000))
			.andExpect(jsonPath("$.suggestion.estimatedCount").value(1));
	}

	@Test
	void sort가_PRICE면_순위가_실제로_달라진다() throws Exception {
		// SCORE 1위 후보: 중간가(3200)지만 매우 가까움(30m 상당)
		Pharmacy scoreTop = pharmacy("스코어1위", BASE_LAT + 0.00027, BASE_LNG);
		stat(scoreTop, 3200, 3200, 3200, 3200, 4, LocalDate.now());
		// PRICE 1위 후보: 최저가(3000)지만 멀리(1900m 상당, 2km 반경 안)
		Pharmacy priceTop = pharmacy("가격1위", BASE_LAT + 0.01707, BASE_LNG);
		stat(priceTop, 3000, 3000, 3000, 3000, 4, LocalDate.now());
		// 가격 정규화 기준점을 끌어올리는 최고가 미끼(1000m 상당)
		Pharmacy anchor = pharmacy("최고가", BASE_LAT + 0.00898, BASE_LNG);
		stat(anchor, 4500, 4500, 4500, 4500, 4, LocalDate.now());

		mockMvc.perform(get("/api/v1/search")
				.param("drugId", String.valueOf(drug.getId()))
				.param("lat", String.valueOf(BASE_LAT))
				.param("lng", String.valueOf(BASE_LNG)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.results[0].pharmacy.name").value("스코어1위"));

		mockMvc.perform(get("/api/v1/search")
				.param("drugId", String.valueOf(drug.getId()))
				.param("lat", String.valueOf(BASE_LAT))
				.param("lng", String.valueOf(BASE_LNG))
				.param("sort", "PRICE"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.results[0].pharmacy.name").value("가격1위"));
	}

	@Test
	void 가격_통계가_없는_약국은_결과에_나타나지_않는다() throws Exception {
		pharmacy("무통계약국", BASE_LAT, BASE_LNG);
		// pharmacy_drug_price_stat row 없음 (제보 집계 전 상태) -> INNER JOIN으로 자동 제외

		mockMvc.perform(get("/api/v1/search")
				.param("drugId", String.valueOf(drug.getId()))
				.param("lat", String.valueOf(BASE_LAT))
				.param("lng", String.valueOf(BASE_LNG)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.summary.resultCount").value(0));
	}

	@Test
	void regionCode만으로도_검색된다() throws Exception {
		Pharmacy near = pharmacy("동네약국", BASE_LAT, BASE_LNG);
		stat(near, 3000, 3000, 3000, 3000, 2, LocalDate.now());

		mockMvc.perform(get("/api/v1/search")
				.param("drugId", String.valueOf(drug.getId()))
				.param("regionCode", region.getCode()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.query.locationSource").value("REGION"))
			.andExpect(jsonPath("$.summary.resultCount").value(1));
	}
}
