package com.pharmaprice.pharmacy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "region")
@Getter
@NoArgsConstructor
public class Region {

	@Id
	@Column(length = 10)
	private String code;

	@Column(nullable = false, length = 20)
	private String sido;

	@Column(nullable = false, length = 30)
	private String sigungu;

	@Column(name = "center_lat", nullable = false)
	private Double centerLat;

	@Column(name = "center_lng", nullable = false)
	private Double centerLng;

	@Builder
	public Region(String code, String sido, String sigungu, Double centerLat, Double centerLng) {
		this.code = code;
		this.sido = sido;
		this.sigungu = sigungu;
		this.centerLat = centerLat;
		this.centerLng = centerLng;
	}
}
