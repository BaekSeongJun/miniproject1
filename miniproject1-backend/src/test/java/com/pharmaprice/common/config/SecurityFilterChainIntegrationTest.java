package com.pharmaprice.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SecurityFilterChainIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private JwtTokenProvider jwtTokenProvider;
	@Autowired
	private AppUserRepository appUserRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private DrugRepository drugRepository;

	private AppUser userAccount;

	@BeforeEach
	void setUp() {
		userAccount = appUserRepository.save(AppUser.builder()
			.email("user@example.com").passwordHash(passwordEncoder.encode("pw"))
			.nickname("유저").role(UserRole.USER).status(UserStatus.ACTIVE).reportCount(0).build());
	}

	@Test
	void 토큰_없이_인증필요_경로_호출시_401_UNAUTHENTICATED() throws Exception {
		mockMvc.perform(post("/api/v1/price-reports"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void USER_토큰으로_admin_경로_호출시_403_FORBIDDEN() throws Exception {
		String token = jwtTokenProvider.createAccessToken(userAccount);

		mockMvc.perform(get("/api/v1/admin/stats/overview")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void 만료된_토큰으로_호출시_401() throws Exception {
		String expiredToken = jwtTokenProvider.createAccessToken(userAccount, Duration.ofSeconds(-1));

		mockMvc.perform(post("/api/v1/price-reports")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void GET_search는_토큰_없이_200() throws Exception {
		Drug drug = drugRepository.save(Drug.builder()
			.itemSeq("999999999").name("테스트약").displayName("테스트약")
			.category("기타").packageUnit("1개").otcFlag(true).build());

		mockMvc.perform(get("/api/v1/search")
				.param("drugId", String.valueOf(drug.getId()))
				.param("lat", "37.4979")
				.param("lng", "127.0276"))
			.andExpect(status().isOk());
	}

	@Test
	void ADMIN_토큰으로_admin_경로_호출시_401_403_아님() throws Exception {
		AppUser adminAccount = appUserRepository.save(AppUser.builder()
			.email("admin-test@example.com").passwordHash(passwordEncoder.encode("pw"))
			.nickname("관리자").role(UserRole.ADMIN).status(UserStatus.ACTIVE).reportCount(0).build());
		String token = jwtTokenProvider.createAccessToken(adminAccount);

		var result = mockMvc.perform(get("/api/v1/admin/stats/overview")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andReturn();

		assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
	}
}
