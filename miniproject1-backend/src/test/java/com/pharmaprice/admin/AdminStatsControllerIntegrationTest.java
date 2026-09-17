package com.pharmaprice.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaprice.AbstractIntegrationTest;
import com.pharmaprice.admin.repository.AdminStatsRepository;
import com.pharmaprice.auth.domain.AppUser;
import com.pharmaprice.auth.domain.UserRole;
import com.pharmaprice.auth.domain.UserStatus;
import com.pharmaprice.auth.repository.AppUserRepository;
import com.pharmaprice.auth.security.JwtTokenProvider;
import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.drug.repository.DrugRepository;
import com.pharmaprice.pharmacy.domain.Pharmacy;
import com.pharmaprice.pharmacy.domain.Region;
import com.pharmaprice.pharmacy.repository.PharmacyRepository;
import com.pharmaprice.pharmacy.repository.RegionRepository;
import com.pharmaprice.recommendation.domain.PharmacyDrugPriceStat;
import com.pharmaprice.recommendation.repository.PriceStatRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AdminStatsControllerIntegrationTest extends AbstractIntegrationTest {

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
	private AppUserRepository appUserRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private JwtTokenProvider jwtTokenProvider;
	@Autowired
	private AdminStatsRepository adminStatsRepository;

	private String adminToken;
	private Drug drug;
	private long pharmacyCountBefore;

	@BeforeEach
	void setUp() {
		pharmacyCountBefore = adminStatsRepository.countActivePharmacies();

		AppUser admin = appUserRepository.save(AppUser.builder()
			.email("admin-test@example.com").passwordHash(passwordEncoder.encode("pw"))
			.nickname("관리자").role(UserRole.ADMIN).status(UserStatus.ACTIVE).reportCount(0).build());
		adminToken = jwtTokenProvider.createAccessToken(admin);

		drug = drugRepository.save(Drug.builder()
			.itemSeq("196800050").name("타이레놀정500밀리그람").displayName("타이레놀 500mg")
			.category("해열진통").packageUnit("8정").otcFlag(true).build());

		Region cheap = regionRepository.save(Region.builder()
			.code("11350").sido("서울특별시").sigungu("노원구").centerLat(37.65).centerLng(127.06).build());
		Region expensive = regionRepository.save(Region.builder()
			.code("11650").sido("서울특별시").sigungu("서초구").centerLat(37.48).centerLng(127.03).build());
		Region mid = regionRepository.save(Region.builder()
			.code("11740").sido("서울특별시").sigungu("강동구").centerLat(37.53).centerLng(127.12).build());

		// price-gaps는 (1) 지역당 약국 3곳 이상, (2) 약품당 지역 3곳 이상이어야 집계된다 (DATABASE.md §5.4)
		// 시드 데이터에 이미 다른 약품들의 지역 격차가 섞여 있으므로, 격차를 압도적으로 크게 만들어 순위 1위를 보장한다
		stat(pharmacy("노원1", cheap), 100);
		stat(pharmacy("노원2", cheap), 100);
		stat(pharmacy("노원3", cheap), 100);
		stat(pharmacy("강동1", mid), 500);
		stat(pharmacy("강동2", mid), 500);
		stat(pharmacy("강동3", mid), 500);
		stat(pharmacy("서초1", expensive), 100000);
		stat(pharmacy("서초2", expensive), 100000);
		stat(pharmacy("서초3", expensive), 100000);
	}

	private Pharmacy pharmacy(String name, Region region) {
		return pharmacyRepository.save(Pharmacy.builder()
			.name(name).addressRoad("주소 " + name).region(region)
			.lat(region.getCenterLat()).lng(region.getCenterLng()).isActive(true).build());
	}

	private void stat(Pharmacy pharmacy, int repPrice) {
		priceStatRepository.save(PharmacyDrugPriceStat.builder()
			.pharmacy(pharmacy).drug(drug)
			.repPrice(repPrice).minPrice(repPrice).maxPrice(repPrice).avgPrice(repPrice)
			.reportCount(1).lastReportedAt(LocalDate.now()).windowDays((short) 90)
			.calculatedAt(Instant.now()).build());
	}

	@Test
	void overview는_totals와_coverageRate를_반환한다() throws Exception {
		// 시드 데이터가 이미 DB에 있으므로 절대값이 아니라 방금 추가한 9건만큼 늘었는지로 검증한다
		mockMvc.perform(get("/api/v1/admin/stats/overview")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totals.pharmacyCount").value(pharmacyCountBefore + 9))
			.andExpect(jsonPath("$.coverageRate").isNumber())
			.andExpect(jsonPath("$.recentTrend.length()").value(7));
	}

	@Test
	void regions는_drugId로_필터링하면_해당_약품_지역_통계만_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/admin/stats/regions")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.param("drugId", String.valueOf(drug.getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rows[?(@.region.sigungu=='노원구')].avgPrice").value(100))
			.andExpect(jsonPath("$.rows[?(@.region.sigungu=='서초구')].avgPrice").value(100000));
	}

	@Test
	void drugStats는_해당_약품의_전국_통계와_히스토그램을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/admin/stats/drugs/{drugId}", drug.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.drug.displayName").value("타이레놀 500mg"))
			.andExpect(jsonPath("$.national.min").value(100))
			.andExpect(jsonPath("$.national.max").value(100000));
	}

	@Test
	void priceGaps는_gapPct를_수기_검산과_동일하게_반환한다() throws Exception {
		// cheapest 평균 100, priciest 평균 100000 → gap 99900, gapPct = 99900/100*100 = 99900.0 (전체 1위 보장)
		mockMvc.perform(get("/api/v1/admin/stats/price-gaps")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rows[0].drug.displayName").value("타이레놀 500mg"))
			.andExpect(jsonPath("$.rows[0].cheapestRegion.sigungu").value("노원구"))
			.andExpect(jsonPath("$.rows[0].priciestRegion.sigungu").value("서초구"))
			.andExpect(jsonPath("$.rows[0].gap").value(99900))
			.andExpect(jsonPath("$.rows[0].gapPct").value(99900.0));
	}

	@Test
	void 표본이_3건_미만인_지역은_price_gaps에서_제외된다() throws Exception {
		Region tiny = regionRepository.save(Region.builder()
			.code("11110").sido("서울특별시").sigungu("종로구").centerLat(37.57).centerLng(126.98).build());
		stat(pharmacy("종로1", tiny), 100);
		stat(pharmacy("종로2", tiny), 100);

		var result = mockMvc.perform(get("/api/v1/admin/stats/price-gaps")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andReturn();

		assertThat(result.getResponse().getContentAsString()).doesNotContain("종로구");
	}
}
