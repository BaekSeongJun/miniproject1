package com.pharmaprice.drug.dto;

import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.drug.domain.DrugStatus;

public record AdminDrugListItemResponse(
	Long id, String displayName, String category, String packageUnit, DrugStatus status
) {
	public static AdminDrugListItemResponse from(Drug drug) {
		return new AdminDrugListItemResponse(
			drug.getId(), drug.getDisplayName(), drug.getCategory(), drug.getPackageUnit(), drug.getStatus());
	}
}
