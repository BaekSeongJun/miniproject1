package com.pharmaprice.drug.domain;

import com.pharmaprice.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "drug")
@Getter
@NoArgsConstructor
public class Drug extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "item_seq", unique = true, length = 20)
	private String itemSeq;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@Column(length = 100)
	private String maker;

	@Column(nullable = false, length = 50)
	private String category;

	@Column(length = 50)
	private String form;

	@Column(name = "package_unit", nullable = false, length = 50)
	private String packageUnit;

	@Column(name = "otc_flag", nullable = false)
	private Boolean otcFlag;

	@Column(name = "base_price")
	private Integer basePrice;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Builder
	public Drug(String itemSeq, String name, String displayName, String maker, String category, String form,
			String packageUnit, Boolean otcFlag, Integer basePrice, String imageUrl) {
		this.itemSeq = itemSeq;
		this.name = name;
		this.displayName = displayName;
		this.maker = maker;
		this.category = category;
		this.form = form;
		this.packageUnit = packageUnit;
		this.otcFlag = otcFlag;
		this.basePrice = basePrice;
		this.imageUrl = imageUrl;
	}
}
