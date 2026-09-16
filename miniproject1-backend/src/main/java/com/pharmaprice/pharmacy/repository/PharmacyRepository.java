package com.pharmaprice.pharmacy.repository;

import com.pharmaprice.pharmacy.domain.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {
}
