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
import com.pharmaprice.drug.domain.DrugStatus;
import com.pharmaprice.drug.repository.DrugRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AdminDrugControllerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private AppUserRepository appUserRepository;
	@Autowired
	private DrugRepository drugRepository;
	@Autowired
	private JwtTokenProvider jwtTokenProvider;
	@Autowired
	private PasswordEncoder passwordEncoder;

	private String adminToken;

	@BeforeEach
	void setUp() {
		AppUser admin = appUserRepository.save(AppUser.builder()
			.email("admin-drugs@example.com").passwordHash(passwordEncoder.encode("pw"))
			.nickname("관리자").role(UserRole.ADMIN).status(UserStatus.ACTIVE).reportCount(0).build());
		adminToken = jwtTokenProvider.createAccessToken(admin);
	}

	private Drug pendingDrug(String displayName) {
		return drugRepository.save(Drug.builder()
			.name(displayName).displayName(displayName).category("기타").packageUnit("미상")
			.otcFlag(true).status(DrugStatus.PENDING).build());
	}

	@Test
	void status_파라미터_없이_조회하면_PENDING_목록을_반환한다() throws Exception {
		Drug drug = pendingDrug("승인대기약품");
		drugRepository.save(Drug.builder()
			.name("이미승인됨").displayName("이미승인됨").category("기타").packageUnit("미상")
			.otcFlag(true).status(DrugStatus.APPROVED).build());

		mockMvc.perform(get("/api/v1/admin/drugs")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(drug.getId()))
			.andExpect(jsonPath("$.content[0].status").value("PENDING"));
	}

	@Test
	void APPROVED로_승인하면_상태가_바뀐다() throws Exception {
		Drug drug = pendingDrug("승인할약품");

		mockMvc.perform(patch("/api/v1/admin/drugs/{id}", drug.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"APPROVED\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APPROVED"));

		Drug updated = drugRepository.findById(drug.getId()).orElseThrow();
		assertThat(updated.getStatus()).isEqualTo(DrugStatus.APPROVED);
	}

	@Test
	void REJECTED로_반려하면_상태가_바뀐다() throws Exception {
		Drug drug = pendingDrug("반려할약품");

		mockMvc.perform(patch("/api/v1/admin/drugs/{id}", drug.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"REJECTED\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void USER_토큰으로_호출시_403() throws Exception {
		AppUser normalUser = appUserRepository.save(AppUser.builder()
			.email("user-drugs@example.com").passwordHash(passwordEncoder.encode("pw"))
			.nickname("일반유저").role(UserRole.USER).status(UserStatus.ACTIVE).reportCount(0).build());
		String userToken = jwtTokenProvider.createAccessToken(normalUser);

		mockMvc.perform(get("/api/v1/admin/drugs")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
			.andExpect(status().isForbidden());
	}
}
