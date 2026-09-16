package com.pharmaprice.drug.service;

import com.pharmaprice.common.dto.PageResponse;
import com.pharmaprice.drug.dto.DrugDetailResponse;
import com.pharmaprice.drug.dto.DrugSummaryResponse;
import com.pharmaprice.drug.repository.DrugQueryRepository;
import com.pharmaprice.drug.repository.DrugQueryRepository.DrugDetailProjection;
import com.pharmaprice.drug.repository.DrugQueryRepository.DrugSummaryProjection;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DrugService {

	private static final int MAX_PAGE_SIZE = 50;

	private final DrugQueryRepository drugQueryRepository;

	public PageResponse<DrugSummaryResponse> search(String q, String category, int page, int size) {
		Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
		Page<DrugSummaryProjection> result = drugQueryRepository.search(q, category, pageable);
		return PageResponse.of(result.map(this::toSummaryResponse));
	}

	public DrugDetailResponse findDetail(long drugId) {
		DrugDetailProjection p = drugQueryRepository.findDetailById(drugId)
			.orElseThrow(() -> new NoSuchElementException("DRUG_NOT_FOUND"));
		return new DrugDetailResponse(
			p.getId(), p.getItemSeq(), p.getDisplayName(), p.getName(), p.getMaker(),
			p.getCategory(), p.getForm(), p.getPackageUnit(), p.getImageUrl(),
			new DrugDetailResponse.PriceStats(
				p.getNationalAvg(), p.getNationalMin(), p.getNationalMax(),
				p.getPharmacyCount(), p.getReportCount()));
	}

	private DrugSummaryResponse toSummaryResponse(DrugSummaryProjection p) {
		return new DrugSummaryResponse(
			p.getId(), p.getItemSeq(), p.getDisplayName(), p.getName(), p.getMaker(),
			p.getCategory(), p.getForm(), p.getPackageUnit(), p.getImageUrl(),
			p.getNationalAvgPrice(), p.getPharmacyCount());
	}
}
