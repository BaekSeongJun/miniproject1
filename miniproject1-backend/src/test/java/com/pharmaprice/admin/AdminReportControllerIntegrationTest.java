package com.pharmaprice.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaprice.AbstractIntegrationTest;
import com.pharmaprice.auth.domain.AppUser;
import com.pharmaprice.auth.domain.UserRole;
import com.pharmaprice.auth.domain.UserStatus;
import com.pharmaprice.auth.repository.AppUserRepository;
import com.pharmaprice.auth.security.JwtTokenProvider;
import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.drug.repository.DrugRepository;
import com.pharmaprice.pharmacy.domain.Pharmacy;
import com.pharmaprice.pharmacy.repository.PharmacyRepository;
import com.pharmaprice.recommendation.repository.PriceStatRepository;
import com.pharmaprice.report.domain.PriceReport;
import com.pharmaprice.report.domain.ReportSource;
import com.pharmaprice.report.domain.ReportStatus;
import com.pharmaprice.report.repository.PriceReportRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AdminReportControllerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private AppUserRepository appUserRepository;
	@Autowired
	private PharmacyRepository pharmacyRepository;
	@Autowired
	private DrugRepository drugRepository;
	@Autowired
	private PriceReportRepository priceReportRepository;
	@Autowired
	private PriceStatRepository priceStatRepository;
	@Autowired
	private JwtTokenProvider jwtTokenProvider;
	@Autowired
	private PasswordEncoder passwordEncoder;

	private String adminToken;
	private Pharmacy pharmacy;
	private Drug drug;
	private int reporterSeq;

	@BeforeEach
	void setUp() {
		AppUser admin = appUserRepository.save(AppUser.builder()
			.email("admin-t32@example.com").passwordHash(passwordEncoder.encode("pw"))
			.nickname("관리자").role(UserRole.ADMIN).status(UserStatus.ACTIVE).reportCount(0).build());
		adminToken = jwtTokenProvider.createAccessToken(admin);

		pharmacy = pharmacyRepository.save(Pharmacy.builder()
			.name("가온약국").lat(37.5).lng(127.0).isActive(true).build());
		drug = drugRepository.save(Drug.builder()
			.itemSeq("196800050").name("타이레놀정500밀리그람").displayName("타이레놀 500mg")
			.category("해열진통").packageUnit("8정").otcFlag(true).build());
	}

	// 같은 (user, pharmacy, drug, day) 조합은 uq_report_user_pair_day에 걸리므로, 매번 다른 제보자를 만들어 등록한다
	private PriceReport report(int price, ReportStatus status, boolean flagged) {
		AppUser user = appUserRepository.save(AppUser.builder()
			.email("reporter-t32-" + (reporterSeq++) + "@example.com").passwordHash(passwordEncoder.encode("pw"))
			.nickname("제보자").role(UserRole.USER).status(UserStatus.ACTIVE).reportCount(1).build());
		return priceReportRepository.save(PriceReport.builder()
			.pharmacy(pharmacy).drug(drug).user(user)
			.price(price).purchasedAt(LocalDate.now())
			.source(ReportSource.FORM).status(status).flagged(flagged).build());
	}

	@Test
	void list는_flagged와_status로_필터링하고_reporter_email을_포함한다() throws Exception {
		report(2800, ReportStatus.ACTIVE, false);
		PriceReport flaggedReport = report(9999, ReportStatus.ACTIVE, true);

		mockMvc.perform(get("/api/v1/admin/price-reports")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.param("flagged", "true")
				.param("pharmacyId", String.valueOf(pharmacy.getId()))
				.param("drugId", String.valueOf(drug.getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(flaggedReport.getId()))
			.andExpect(jsonPath("$.content[0].reporter.email").value(flaggedReport.getUser().getEmail()))
			.andExpect(jsonPath("$.content[0].flagged").value(true));
	}

	@Test
	void 제보를_HIDDEN으로_바꾸면_통계가_즉시_재계산된다() throws Exception {
		report(2800, ReportStatus.ACTIVE, false);
		PriceReport target = report(3200, ReportStatus.ACTIVE, false);
		// PriceStatService.recalculate는 PATCH 시점에 호출되므로, 사전 상태 스냅샷은 굳이 만들 필요 없다

		mockMvc.perform(patch("/api/v1/admin/price-reports/{id}", target.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"HIDDEN\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("HIDDEN"))
			.andExpect(jsonPath("$.recalculatedStat.repPrice").value(2800))
			.andExpect(jsonPath("$.recalculatedStat.reportCount").value(1));

		var stat = priceStatRepository.findByPharmacyIdAndDrugId(pharmacy.getId(), drug.getId()).orElseThrow();
		assertThat(stat.getRepPrice()).isEqualTo(2800);
		assertThat(stat.getReportCount()).isEqualTo(1);
	}

	@Test
	void flagged를_false로_풀면_통계에_재포함된다() throws Exception {
		PriceReport target = report(3200, ReportStatus.ACTIVE, true);

		mockMvc.perform(patch("/api/v1/admin/price-reports/{id}", target.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"flagged\":false}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.flagged").value(false))
			.andExpect(jsonPath("$.recalculatedStat.reportCount").value(1));

		var stat = priceStatRepository.findByPharmacyIdAndDrugId(pharmacy.getId(), drug.getId()).orElseThrow();
		assertThat(stat.getReportCount()).isEqualTo(1);
	}

	@Test
	void REJECTED로_바꾸면_제보자의_report_count가_1_차감된다() throws Exception {
		PriceReport target = report(3200, ReportStatus.ACTIVE, false);

		mockMvc.perform(patch("/api/v1/admin/price-reports/{id}", target.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"REJECTED\",\"reason\":\"MANUAL\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REJECTED"));

		AppUser updated = appUserRepository.findById(target.getUser().getId()).orElseThrow();
		assertThat(updated.getReportCount()).isEqualTo(0);
	}

	@Test
	void keyword로_약국명_약품명_제보자를_검색한다() throws Exception {
		PriceReport target = report(2800, ReportStatus.ACTIVE, false);

		// 시드 데이터가 전국 규모라 keyword가 다른 약국명에도 부분일치할 수 있으므로,
		// 건수를 단정하지 않고 pharmacyId로 좁혀 정확히 그 제보가 나오는지만 확인한다.
		mockMvc.perform(get("/api/v1/admin/price-reports")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.param("keyword", "가온")
				.param("pharmacyId", String.valueOf(pharmacy.getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(target.getId()))
			.andExpect(jsonPath("$.content[0].pharmacy.name").value("가온약국"));

		mockMvc.perform(get("/api/v1/admin/price-reports")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.param("keyword", "가온")
				.param("pharmacyId", String.valueOf(pharmacy.getId()))
				.param("drugId", "999999999"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void USER_토큰으로_admin_제보관리_호출시_403() throws Exception {
		AppUser normalUser = appUserRepository.save(AppUser.builder()
			.email("user-t32@example.com").passwordHash(passwordEncoder.encode("pw"))
			.nickname("일반유저").role(UserRole.USER).status(UserStatus.ACTIVE).reportCount(0).build());
		String userToken = jwtTokenProvider.createAccessToken(normalUser);

		mockMvc.perform(get("/api/v1/admin/price-reports")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
			.andExpect(status().isForbidden());
	}
}
