package com.pharmaprice.pharmacy;

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
class PharmacyControllerIntegrationTest extends AbstractIntegrationTest {

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

	@Test
	void q와_lat_lng가_모두_없으면_400() throws Exception {
		mockMvc.perform(get("/api/v1/pharmacies"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 이름으로_검색하면_주소_부분일치도_매칭된다() throws Exception {
		pharmacyRepository.save(Pharmacy.builder()
			.name("가온약국").addressRoad("서울시 강남구 테헤란로").region(region)
			.lat(BASE_LAT).lng(BASE_LNG).isActive(true).build());

		mockMvc.perform(get("/api/v1/pharmacies").param("q", "가온"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].name").value("가온약국"))
			.andExpect(jsonPath("$.content[0].distanceM").doesNotExist());
	}

	@Test
	void 좌표로_검색하면_distanceM이_채워진다() throws Exception {
		pharmacyRepository.save(Pharmacy.builder()
			.name("가까운약국").addressRoad("서울시 강남구").region(region)
			.lat(BASE_LAT + 0.001).lng(BASE_LNG).isActive(true).build());

		mockMvc.perform(get("/api/v1/pharmacies")
				.param("lat", String.valueOf(BASE_LAT))
				.param("lng", String.valueOf(BASE_LNG)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].distanceM").isNumber());
	}

	@Test
	void 대한민국_범위_밖_좌표는_400_INVALID_COORDINATE() throws Exception {
		mockMvc.perform(get("/api/v1/pharmacies")
				.param("lat", "1.0")
				.param("lng", "1.0"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 상세조회시_drugPrices가_대표가격_오름차순이고_diff가_계산된다() throws Exception {
		Pharmacy pharmacy = pharmacyRepository.save(Pharmacy.builder()
			.name("가온약국").addressRoad("서울시 강남구").addressJibun("서울시 강남구 역삼동")
			.region(region).lat(BASE_LAT).lng(BASE_LNG).phone("02-555-1234").isActive(true).build());

		Drug drug2 = drugRepository.save(Drug.builder()
			.itemSeq("999900002").name("게보린정").displayName("게보린")
			.maker("삼진제약").category("해열진통").form("정제").packageUnit("10정")
			.otcFlag(true).basePrice(4000).build());

		// 타이레놀: 이 약국만 2800원 (전국 평균 = 자기 자신뿐이라 2800)
		priceStatRepository.save(PharmacyDrugPriceStat.builder()
			.pharmacy(pharmacy).drug(drug)
			.repPrice(2800).minPrice(2700).maxPrice(3000).avgPrice(2830)
			.reportCount(4).lastReportedAt(LocalDate.now().minusDays(3)).windowDays((short) 90)
			.calculatedAt(Instant.now()).build());
		// 게보린: 3500원, 다른 약국은 4500원 -> 전국 평균 4000, diff = -500
		priceStatRepository.save(PharmacyDrugPriceStat.builder()
			.pharmacy(pharmacy).drug(drug2)
			.repPrice(3500).minPrice(3500).maxPrice(3500).avgPrice(3500)
			.reportCount(2).lastReportedAt(LocalDate.now().minusDays(1)).windowDays((short) 90)
			.calculatedAt(Instant.now()).build());
		Pharmacy other = pharmacyRepository.save(Pharmacy.builder()
			.name("다른약국").addressRoad("서울시 강남구").region(region)
			.lat(BASE_LAT).lng(BASE_LNG).isActive(true).build());
		priceStatRepository.save(PharmacyDrugPriceStat.builder()
			.pharmacy(other).drug(drug2)
			.repPrice(4500).minPrice(4500).maxPrice(4500).avgPrice(4500)
			.reportCount(1).lastReportedAt(LocalDate.now()).windowDays((short) 90)
			.calculatedAt(Instant.now()).build());

		mockMvc.perform(get("/api/v1/pharmacies/{id}", pharmacy.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("가온약국"))
			.andExpect(jsonPath("$.region.code").value("11680"))
			.andExpect(jsonPath("$.drugPrices.length()").value(2))
			.andExpect(jsonPath("$.drugPrices[0].repPrice").value(2800))
			.andExpect(jsonPath("$.drugPrices[1].repPrice").value(3500))
			.andExpect(jsonPath("$.drugPrices[1].nationalAvgPrice").value(4000))
			.andExpect(jsonPath("$.drugPrices[1].diffFromNationalAvg").value(-500));
	}

	@Test
	void 존재하지_않는_약국은_404() throws Exception {
		mockMvc.perform(get("/api/v1/pharmacies/{id}", 999999))
			.andExpect(status().isNotFound());
	}

	@Test
	void 가격이력에_flagged_포함되고_HIDDEN은_제외된다() throws Exception {
		Pharmacy pharmacy = pharmacyRepository.save(Pharmacy.builder()
			.name("가온약국").addressRoad("서울시 강남구").region(region)
			.lat(BASE_LAT).lng(BASE_LNG).isActive(true).build());

		priceReportRepository.save(PriceReport.builder()
			.pharmacy(pharmacy).drug(drug).price(2800).purchasedAt(LocalDate.now().minusDays(10))
			.source(ReportSource.SEED).status(ReportStatus.ACTIVE).flagged(false).build());
		priceReportRepository.save(PriceReport.builder()
			.pharmacy(pharmacy).drug(drug).price(9900).purchasedAt(LocalDate.now().minusDays(5))
			.source(ReportSource.SEED).status(ReportStatus.ACTIVE).flagged(true).build());
		priceReportRepository.save(PriceReport.builder()
			.pharmacy(pharmacy).drug(drug).price(2700).purchasedAt(LocalDate.now().minusDays(1))
			.source(ReportSource.SEED).status(ReportStatus.HIDDEN).flagged(false).build());

		mockMvc.perform(get("/api/v1/pharmacies/{pharmacyId}/drugs/{drugId}/history", pharmacy.getId(), drug.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.points.length()").value(2))
			.andExpect(jsonPath("$.points[0].flagged").value(false))
			.andExpect(jsonPath("$.points[1].flagged").value(true));
	}

	@Test
	void days로_범위를_좁히면_결과가_줄어든다() throws Exception {
		Pharmacy pharmacy = pharmacyRepository.save(Pharmacy.builder()
			.name("가온약국").addressRoad("서울시 강남구").region(region)
			.lat(BASE_LAT).lng(BASE_LNG).isActive(true).build());

		priceReportRepository.save(PriceReport.builder()
			.pharmacy(pharmacy).drug(drug).price(2800).purchasedAt(LocalDate.now().minusDays(100))
			.source(ReportSource.SEED).status(ReportStatus.ACTIVE).flagged(false).build());
		priceReportRepository.save(PriceReport.builder()
			.pharmacy(pharmacy).drug(drug).price(2700).purchasedAt(LocalDate.now().minusDays(5))
			.source(ReportSource.SEED).status(ReportStatus.ACTIVE).flagged(false).build());

		mockMvc.perform(get("/api/v1/pharmacies/{pharmacyId}/drugs/{drugId}/history", pharmacy.getId(), drug.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.points.length()").value(2));

		mockMvc.perform(get("/api/v1/pharmacies/{pharmacyId}/drugs/{drugId}/history", pharmacy.getId(), drug.getId())
				.param("days", "30"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.points.length()").value(1));
	}

	@Test
	void 이력이_0건이어도_빈배열과_200() throws Exception {
		Pharmacy pharmacy = pharmacyRepository.save(Pharmacy.builder()
			.name("빈이력약국").addressRoad("서울시 강남구").region(region)
			.lat(BASE_LAT).lng(BASE_LNG).isActive(true).build());

		mockMvc.perform(get("/api/v1/pharmacies/{pharmacyId}/drugs/{drugId}/history", pharmacy.getId(), drug.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.points").isArray())
			.andExpect(jsonPath("$.points.length()").value(0));
	}
}
