package com.pharmaprice.drug;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaprice.AbstractIntegrationTest;
import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.drug.repository.DrugRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class DrugControllerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private DrugRepository drugRepository;

	private Drug etcDrug;

	@BeforeEach
	void setUp() {
		drugRepository.save(Drug.builder()
			.itemSeq("196800050").name("타이레놀정500밀리그람").displayName("타이레놀 500mg")
			.maker("한국얀센").category("해열진통").form("정제").packageUnit("8정")
			.otcFlag(true).basePrice(3000).build());
		etcDrug = drugRepository.save(Drug.builder()
			.itemSeq("999900001").name("전문의약품테스트").displayName("전문약")
			.maker("테스트제약").category("기타").form("정제").packageUnit("1정")
			.otcFlag(false).basePrice(10000).build());
	}

	@Test
	void 검색어로_일반의약품을_찾으면_200과_1건_이상_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/drugs").param("q", "타이레놀"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
			.andExpect(jsonPath("$.content[0].displayName").value("타이레놀 500mg"))
			.andExpect(jsonPath("$.content[0].itemSeq").value("196800050"));
	}

	@Test
	void 전문의약품은_item_seq로_조회해도_결과에_없다() throws Exception {
		mockMvc.perform(get("/api/v1/drugs").param("q", "전문약"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void size는_최대_50으로_clamp된다() throws Exception {
		mockMvc.perform(get("/api/v1/drugs").param("size", "200"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.size").value(50));
	}
}
