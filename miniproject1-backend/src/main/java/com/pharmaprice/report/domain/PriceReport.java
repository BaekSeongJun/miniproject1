package com.pharmaprice.report.domain;

import com.pharmaprice.auth.domain.AppUser;
import com.pharmaprice.common.domain.BaseTimeEntity;
import com.pharmaprice.drug.domain.Drug;
import com.pharmaprice.pharmacy.domain.Pharmacy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

// uq_report_user_pair_day (표현식 기반 부분 유니크 인덱스)는 V1__init.sql에서만 관리, JPA 매핑 대상 아님
@Entity
@Table(name = "price_report")
@Getter
@NoArgsConstructor
public class PriceReport extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pharmacy_id", nullable = false)
	private Pharmacy pharmacy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "drug_id", nullable = false)
	private Drug drug;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private AppUser user;

	@Column(nullable = false)
	private Integer price;

	@Column(name = "purchased_at", nullable = false)
	private LocalDate purchasedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReportSource source;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReportStatus status;

	@Column(nullable = false)
	private Boolean flagged;

	@Enumerated(EnumType.STRING)
	@Column(name = "flag_reason", length = 100)
	private FlagReason flagReason;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receipt_file_id")
	private UploadedFile receiptFile;

	@Column(length = 200)
	private String memo;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Builder
	public PriceReport(Pharmacy pharmacy, Drug drug, AppUser user, Integer price, LocalDate purchasedAt,
			ReportSource source, ReportStatus status, Boolean flagged, FlagReason flagReason,
			UploadedFile receiptFile, String memo) {
		this.pharmacy = pharmacy;
		this.drug = drug;
		this.user = user;
		this.price = price;
		this.purchasedAt = purchasedAt;
		this.source = source;
		this.status = status;
		this.flagged = flagged;
		this.flagReason = flagReason;
		this.receiptFile = receiptFile;
		this.memo = memo;
	}
}
