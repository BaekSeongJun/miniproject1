package com.pharmaprice.report;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaprice.AbstractIntegrationTest;
import com.pharmaprice.auth.domain.AppUser;
import com.pharmaprice.auth.domain.UserRole;
import com.pharmaprice.auth.domain.UserStatus;
import com.pharmaprice.auth.repository.AppUserRepository;
import com.pharmaprice.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class UploadControllerIntegrationTest extends AbstractIntegrationTest {

	private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private AppUserRepository appUserRepository;
	@Autowired
	private JwtTokenProvider jwtTokenProvider;
	@Autowired
	private PasswordEncoder passwordEncoder;

	private String ownerToken;
	private String otherToken;

	@BeforeEach
	void setUp() {
		AppUser owner = appUserRepository.save(AppUser.builder()
			.email("owner@example.com").passwordHash(passwordEncoder.encode("pw"))
			.nickname("소유자").role(UserRole.USER).status(UserStatus.ACTIVE).reportCount(0).build());
		AppUser other = appUserRepository.save(AppUser.builder()
			.email("other@example.com").passwordHash(passwordEncoder.encode("pw"))
			.nickname("타인").role(UserRole.USER).status(UserStatus.ACTIVE).reportCount(0).build());
		ownerToken = jwtTokenProvider.createAccessToken(owner);
		otherToken = jwtTokenProvider.createAccessToken(other);
	}

	@Test
	void 정상_이미지_업로드시_201() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", JPEG_BYTES);

		mockMvc.perform(multipart("/api/v1/uploads")
				.file(file)
				.param("purpose", "RECEIPT")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.contentType").value("image/jpeg"))
			.andExpect(jsonPath("$.url").exists());
	}

	@Test
	void 확장자만_jpg인_PDF는_415_UNSUPPORTED_FILE_TYPE() throws Exception {
		byte[] pdfBytes = {'%', 'P', 'D', 'F', '-', '1', '.', '4'};
		MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", pdfBytes);

		mockMvc.perform(multipart("/api/v1/uploads")
				.file(file)
				.param("purpose", "RECEIPT")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
	}

	@Test
	void 타인의_파일_조회시_403() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", JPEG_BYTES);
		String body = mockMvc.perform(multipart("/api/v1/uploads")
				.file(file)
				.param("purpose", "RECEIPT")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
			.andReturn().getResponse().getContentAsString();
		long fileId = Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));

		mockMvc.perform(get("/api/v1/uploads/" + fileId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	// MockMvc는 서블릿 컨테이너의 MultipartConfigElement 크기 제한을 강제하지 않아
	// spring.servlet.multipart.max-file-size 초과를 여기서 재현할 수 없다.
	// 예외 -> 413 매핑 자체는 GlobalExceptionHandlerTest에서 단위 테스트로 검증한다.

	@Test
	void 본인의_파일_조회시_200() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", JPEG_BYTES);
		String body = mockMvc.perform(multipart("/api/v1/uploads")
				.file(file)
				.param("purpose", "RECEIPT")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
			.andReturn().getResponse().getContentAsString();
		long fileId = Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));

		mockMvc.perform(get("/api/v1/uploads/" + fileId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
			.andExpect(status().isOk());
	}
}
