package com.pharmaprice.report;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class PriceReportControllerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private AppUserRepository appUserRepository;
	@Autowired
	private PharmacyRepository pharmacyRepository;
	@Autowired
	private DrugRepository drugRepository;
	@Autowired
	private JwtTokenProvider jwtTokenProvider;
	@Autowired
	private PasswordEncoder passwordEncoder;

	private String token;
	private Pharmacy pharmacy;
	private Drug otcDrug;
	private Drug etcDrug;

	@BeforeEach
	void setUp() {
		AppUser user = appUserRepository.save(AppUser.builder()
			.email("reporter@example.com").passwordHash(passwordEncoder.encode("pw"))
			.nickname("제보자").role(UserRole.USER).status(UserStatus.ACTIVE).reportCount(0).build());
		token = jwtTokenProvider.createAccessToken(user);

		pharmacy = pharmacyRepository.save(Pharmacy.builder()
			.name("가온약국").lat(37.5).lng(127.0).isActive(true).build());
		otcDrug = drugRepository.save(Drug.builder()
			.itemSeq("196800050").name("타이레놀정500밀리그람").displayName("타이레놀 500mg")
			.category("해열진통").packageUnit("8정").otcFlag(true).build());
		etcDrug = drugRepository.save(Drug.builder()
			.itemSeq("999900001").name("전문의약품테스트").displayName("전문약")
			.category("기타").packageUnit("1정").otcFlag(false).build());
	}

	private String reportJson(long pharmacyId, long drugId, int price, String purchasedAt) {
		String purchasedAtField = purchasedAt != null ? "\"purchasedAt\":\"" + purchasedAt + "\"," : "";
		return "{\"pharmacyId\":" + pharmacyId + ",\"drugId\":" + drugId + ","
			+ purchasedAtField + "\"price\":" + price + "}";
	}

	@Test
	void 정상_제보시_201과_updatedStat_반영() throws Exception {
		mockMvc.perform(post("/api/v1/price-reports")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(reportJson(pharmacy.getId(), otcDrug.getId(), 2800, null)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.flagged").value(false))
			.andExpect(jsonPath("$.updatedStat.repPrice").value(2800))
			.andExpect(jsonPath("$.updatedStat.reportCount").value(1));
	}

	@Test
	void 같은_날_같은_조합_재제보시_409_DUPLICATE_REPORT() throws Exception {
		String body = reportJson(pharmacy.getId(), otcDrug.getId(), 2800, null);
		mockMvc.perform(post("/api/v1/price-reports")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/price-reports")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_REPORT"));
	}

	@Test
	void 전문의약품_제보시도시_422_DRUG_NOT_OTC() throws Exception {
		mockMvc.perform(post("/api/v1/price-reports")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(reportJson(pharmacy.getId(), etcDrug.getId(), 5000, null)))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("DRUG_NOT_OTC"));
	}

	@Test
	void 미래_구매일_제보시_400_INVALID_DATE_RANGE() throws Exception {
		mockMvc.perform(post("/api/v1/price-reports")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(reportJson(pharmacy.getId(), otcDrug.getId(), 2800, "2999-01-01")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
	}

	@Test
	void 이상치_제보시_201이지만_flagged_true이고_rep_price는_불변() throws Exception {
		for (int i = 0; i < 4; i++) {
			Pharmacy p = pharmacyRepository.save(Pharmacy.builder()
				.name("약국" + i).lat(37.5).lng(127.0).isActive(true).build());
			mockMvc.perform(post("/api/v1/price-reports")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(reportJson(p.getId(), otcDrug.getId(), 3000, null)))
				.andExpect(status().isCreated());
		}

		mockMvc.perform(post("/api/v1/price-reports")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(reportJson(pharmacy.getId(), otcDrug.getId(), 50000, null)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.flagged").value(true))
			.andExpect(jsonPath("$.flagReason").value("OUTLIER_HIGH"))
			.andExpect(jsonPath("$.updatedStat.repPrice").doesNotExist());
	}
}
