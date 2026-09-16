package com.pharmaprice.report.repository;

import com.pharmaprice.report.domain.PriceReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceReportRepository extends JpaRepository<PriceReport, Long> {
}
