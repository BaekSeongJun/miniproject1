package com.pharmaprice.pharmacy.domain;

import com.pharmaprice.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pharmacy")
@Getter
@NoArgsConstructor
public class Pharmacy extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "hira_code", unique = true, length = 30)
	private String hiraCode;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "address_road", length = 255)
	private String addressRoad;

	@Column(name = "address_jibun", length = 255)
	private String addressJibun;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "region_code")
	private Region region;

	@Column(nullable = false)
	private Double lat;

	@Column(nullable = false)
	private Double lng;

	@Column(length = 20)
	private String phone;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "business_hours", columnDefinition = "jsonb")
	private Map<String, List<String>> businessHours;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Builder
	public Pharmacy(String hiraCode, String name, String addressRoad, String addressJibun, Region region,
			Double lat, Double lng, String phone, Map<String, List<String>> businessHours, Boolean isActive) {
		this.hiraCode = hiraCode;
		this.name = name;
		this.addressRoad = addressRoad;
		this.addressJibun = addressJibun;
		this.region = region;
		this.lat = lat;
		this.lng = lng;
		this.phone = phone;
		this.businessHours = businessHours;
		this.isActive = isActive;
	}
}
