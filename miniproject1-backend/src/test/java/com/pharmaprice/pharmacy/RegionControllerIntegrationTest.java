package com.pharmaprice.pharmacy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaprice.AbstractIntegrationTest;
import com.pharmaprice.pharmacy.domain.Pharmacy;
import com.pharmaprice.pharmacy.domain.Region;
import com.pharmaprice.pharmacy.repository.PharmacyRepository;
import com.pharmaprice.pharmacy.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class RegionControllerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private RegionRepository regionRepository;
	@Autowired
	private PharmacyRepository pharmacyRepository;

	@BeforeEach
	void setUp() {
		Region gangnam = regionRepository.save(Region.builder()
			.code("11680").sido("서울특별시").sigungu("강남구").centerLat(37.4959).centerLng(127.0664).build());
		regionRepository.save(Region.builder()
			.code("11740").sido("서울특별시").sigungu("강동구").centerLat(37.5301).centerLng(127.1238).build());

		pharmacyRepository.save(Pharmacy.builder()
			.name("활성약국").addressRoad("서울시 강남구").region(gangnam)
			.lat(37.49).lng(127.06).isActive(true).build());
		pharmacyRepository.save(Pharmacy.builder()
			.name("폐업약국").addressRoad("서울시 강남구").region(gangnam)
			.lat(37.49).lng(127.06).isActive(false).build());
	}

	@Test
	void 시도별_그룹_구조로_반환하고_pharmacyCount는_활성_약국만_센다() throws Exception {
		mockMvc.perform(get("/api/v1/regions"))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "max-age=3600"))
			.andExpect(jsonPath("$[?(@.sido=='서울특별시')].sigungus[?(@.code=='11680')].centerLat").value(37.4959))
			.andExpect(jsonPath("$[?(@.sido=='서울특별시')].sigungus[?(@.code=='11680')].centerLng").value(127.0664))
			.andExpect(jsonPath("$[?(@.sido=='서울특별시')].sigungus[?(@.code=='11680')].pharmacyCount").value(1))
			.andExpect(jsonPath("$[?(@.sido=='서울특별시')].sigungus[?(@.code=='11740')].pharmacyCount").value(0));
	}
}
