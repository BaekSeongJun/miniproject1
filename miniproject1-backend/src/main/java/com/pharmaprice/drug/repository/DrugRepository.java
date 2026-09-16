package com.pharmaprice.drug.repository;

import com.pharmaprice.drug.domain.Drug;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrugRepository extends JpaRepository<Drug, Long> {
}
