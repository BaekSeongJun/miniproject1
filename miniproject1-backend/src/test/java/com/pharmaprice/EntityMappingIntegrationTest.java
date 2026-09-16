package com.pharmaprice;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmaprice.auth.domain.AppUser;
import com.pharmaprice.auth.domain.RefreshToken;
import com.pharmaprice.auth.domain.UserRole;
import com.pharmaprice.auth.domain.UserStatus;
import com.pharmaprice.auth.repository.AppUserRepository;
import com.pharmaprice.auth.repository.RefreshTokenRepository;
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
import com.pharmaprice.report.domain.UploadedFile;
import com.pharmaprice.report.repository.PriceReportRepository;
import com.pharmaprice.report.repository.UploadedFileRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class EntityMappingIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private RegionRepository regionRepository;
	@Autowired
	private AppUserRepository appUserRepository;
	@Autowired
	private RefreshTokenRepository refreshTokenRepository;
	@Autowired
	private PharmacyRepository pharmacyRepository;
	@Autowired
	private DrugRepository drugRepository;
	@Autowired
	private UploadedFileRepository uploadedFileRepository;
	@Autowired
	private PriceReportRepository priceReportRepository;
	@Autowired
	private PriceStatRepository priceStatRepository;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Region newRegion() {
		return Region.builder()
			.code("11680")
			.sido("서울특별시")
			.sigungu("강남구")
			.centerLat(37.4979)
			.centerLng(127.0276)
			.build();
	}

	private AppUser newAppUser() {
		return AppUser.builder()
			.email("user@example.com")
			.passwordHash("hash")
			.nickname("tester")
			.role(UserRole.USER)
			.status(UserStatus.ACTIVE)
			.reportCount(0)
			.build();
	}

	private Pharmacy newPharmacy(Region region) {
		return Pharmacy.builder()
			.name("행복약국")
			.region(region)
			.lat(37.4979)
			.lng(127.0276)
			.isActive(true)
			.businessHours(Map.of("mon", List.of("09:00", "19:00")))
			.build();
	}

	private Drug newDrug() {
		return Drug.builder()
			.name("타이레놀정500밀리그람")
			.displayName("타이레놀 500mg")
			.category("해열진통")
			.packageUnit("8정")
			.otcFlag(true)
			.build();
	}

	@Test
	void region_저장하고_조회한다() {
		Region saved = regionRepository.save(newRegion());

		Region found = regionRepository.findById(saved.getCode()).orElseThrow();

		assertThat(found.getSido()).isEqualTo("서울특별시");
		assertThat(found.getSigungu()).isEqualTo("강남구");
	}

	@Test
	void appUser_저장하고_조회한다() {
		AppUser saved = appUserRepository.save(newAppUser());

		AppUser found = appUserRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getRole()).isEqualTo(UserRole.USER);
		assertThat(found.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void refreshToken_저장하고_조회한다() {
		AppUser user = appUserRepository.save(newAppUser());

		RefreshToken saved = refreshTokenRepository.save(RefreshToken.builder()
			.user(user)
			.tokenHash("hash-value")
			.expiresAt(Instant.now().plusSeconds(3600))
			.build());

		RefreshToken found = refreshTokenRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getUser().getId()).isEqualTo(user.getId());
	}

	@Test
	void pharmacy_저장하고_조회한다() {
		Region region = regionRepository.save(newRegion());

		Pharmacy saved = pharmacyRepository.save(newPharmacy(region));

		Pharmacy found = pharmacyRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getBusinessHours()).isEqualTo(Map.of("mon", List.of("09:00", "19:00")));
		assertThat(found.getRegion().getCode()).isEqualTo(region.getCode());
	}

	@Test
	void drug_저장하고_조회한다() {
		Drug saved = drugRepository.save(newDrug());

		Drug found = drugRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getDisplayName()).isEqualTo("타이레놀 500mg");
		assertThat(found.getCategory()).isEqualTo("해열진통");
	}

	@Test
	void uploadedFile_저장하고_조회한다() {
		AppUser user = appUserRepository.save(newAppUser());

		UploadedFile saved = uploadedFileRepository.save(UploadedFile.builder()
			.originalName("receipt.jpg")
			.storedPath("/uploads/2026/09/uuid.jpg")
			.contentType("image/jpeg")
			.sizeBytes(1024L)
			.uploadedBy(user)
			.build());

		UploadedFile found = uploadedFileRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getOriginalName()).isEqualTo("receipt.jpg");
	}

	@Test
	void priceReport_저장하고_조회한다() {
		Region region = regionRepository.save(newRegion());
		Pharmacy pharmacy = pharmacyRepository.save(newPharmacy(region));
		Drug drug = drugRepository.save(newDrug());

		PriceReport saved = priceReportRepository.save(PriceReport.builder()
			.pharmacy(pharmacy)
			.drug(drug)
			.price(3000)
			.purchasedAt(LocalDate.now())
			.source(ReportSource.SEED)
			.status(ReportStatus.ACTIVE)
			.flagged(false)
			.build());

		PriceReport found = priceReportRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getStatus()).isEqualTo(ReportStatus.ACTIVE);

		String rawStatus = jdbcTemplate.queryForObject(
			"SELECT status FROM price_report WHERE id = ?", String.class, saved.getId());
		assertThat(rawStatus).isEqualTo("ACTIVE");
	}

	@Test
	void pharmacyDrugPriceStat_저장하고_조회한다() {
		Region region = regionRepository.save(newRegion());
		Pharmacy pharmacy = pharmacyRepository.save(newPharmacy(region));
		Drug drug = drugRepository.save(newDrug());

		PharmacyDrugPriceStat saved = priceStatRepository.save(PharmacyDrugPriceStat.builder()
			.pharmacy(pharmacy)
			.drug(drug)
			.repPrice(3000)
			.minPrice(2800)
			.maxPrice(3200)
			.avgPrice(3000)
			.reportCount(5)
			.lastReportedAt(LocalDate.now())
			.windowDays((short) 90)
			.calculatedAt(Instant.now())
			.build());

		PharmacyDrugPriceStat found = priceStatRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getRepPrice()).isEqualTo(3000);
		assertThat(found.getWindowDays()).isEqualTo((short) 90);
	}
}
