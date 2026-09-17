package com.pharmaprice.drug.dto;

import com.pharmaprice.drug.domain.DrugStatus;
import jakarta.validation.constraints.NotNull;

public record AdminDrugUpdateRequest(@NotNull DrugStatus status) {
}
