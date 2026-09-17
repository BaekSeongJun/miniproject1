package com.pharmaprice.admin.controller;

import com.pharmaprice.admin.service.AdminDrugService;
import com.pharmaprice.common.dto.PageResponse;
import com.pharmaprice.drug.domain.DrugStatus;
import com.pharmaprice.drug.dto.AdminDrugListItemResponse;
import com.pharmaprice.drug.dto.AdminDrugUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/drugs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDrugController {

	private final AdminDrugService adminDrugService;

	@GetMapping
	public PageResponse<AdminDrugListItemResponse> list(
			@RequestParam(required = false) DrugStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return adminDrugService.list(status, page, size);
	}

	@PatchMapping("/{drugId}")
	public AdminDrugListItemResponse update(@PathVariable long drugId, @Valid @RequestBody AdminDrugUpdateRequest request) {
		return adminDrugService.update(drugId, request);
	}
}
