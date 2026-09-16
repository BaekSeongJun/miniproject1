package com.pharmaprice.recommendation.domain;

import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.pharmacy.domain.Pharmacy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pharmacy_drug_price_stat",
	uniqueConstraints = @UniqueConstraint(name = "uq_stat_pair", columnNames = {"pharmacy_id", "drug_id"}))
@Getter
@NoArgsConstructor
public class PharmacyDrugPriceStat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pharmacy_id", nullable = false)
	private Pharmacy pharmacy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "drug_id", nullable = false)
	private Drug drug;

	@Column(name = "rep_price", nullable = false)
	private Integer repPrice;

	@Column(name = "min_price", nullable = false)
	private Integer minPrice;

	@Column(name = "max_price", nullable = false)
	private Integer maxPrice;

	@Column(name = "avg_price", nullable = false)
	private Integer avgPrice;

	@Column(name = "report_count", nullable = false)
	private Integer reportCount;

	@Column(name = "last_reported_at", nullable = false)
	private LocalDate lastReportedAt;

	@Column(name = "window_days", nullable = false)
	private Short windowDays;

	@Column(name = "calculated_at", nullable = false)
	private Instant calculatedAt;

	@Builder
	public PharmacyDrugPriceStat(Pharmacy pharmacy, Drug drug, Integer repPrice, Integer minPrice, Integer maxPrice,
			Integer avgPrice, Integer reportCount, LocalDate lastReportedAt, Short windowDays, Instant calculatedAt) {
		this.pharmacy = pharmacy;
		this.drug = drug;
		this.repPrice = repPrice;
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
		this.avgPrice = avgPrice;
		this.reportCount = reportCount;
		this.lastReportedAt = lastReportedAt;
		this.windowDays = windowDays;
		this.calculatedAt = calculatedAt;
	}
}
