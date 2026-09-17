package com.pharmaprice.admin.service;

import com.pharmaprice.common.dto.PageResponse;
import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.drug.domain.DrugStatus;
import com.pharmaprice.drug.dto.AdminDrugListItemResponse;
import com.pharmaprice.drug.dto.AdminDrugUpdateRequest;
import com.pharmaprice.drug.repository.DrugRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminDrugService {

	private static final int MAX_PAGE_SIZE = 50;

	private final DrugRepository drugRepository;

	@Transactional(readOnly = true)
	public PageResponse<AdminDrugListItemResponse> list(DrugStatus status, int page, int size) {
		Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
		DrugStatus effectiveStatus = status != null ? status : DrugStatus.PENDING;
		return PageResponse.of(drugRepository.findByStatus(effectiveStatus, pageable).map(AdminDrugListItemResponse::from));
	}

	public AdminDrugListItemResponse update(long drugId, AdminDrugUpdateRequest request) {
		Drug drug = drugRepository.findById(drugId)
			.orElseThrow(() -> new NoSuchElementException("DRUG_NOT_FOUND"));
		switch (request.status()) {
			case APPROVED -> drug.approve();
			case REJECTED -> drug.reject();
			case PENDING -> throw new IllegalArgumentException("VALIDATION_FAILED: PENDING으로는 되돌릴 수 없습니다");
		}
		return AdminDrugListItemResponse.from(drug);
	}
}
