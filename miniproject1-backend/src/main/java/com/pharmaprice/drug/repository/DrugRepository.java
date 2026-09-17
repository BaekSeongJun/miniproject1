package com.pharmaprice.drug.repository;

import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.drug.domain.DrugStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrugRepository extends JpaRepository<Drug, Long> {

	Page<Drug> findByStatus(DrugStatus status, Pageable pageable);
}
