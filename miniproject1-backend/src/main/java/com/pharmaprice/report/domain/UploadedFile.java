package com.pharmaprice.report.domain;

import com.pharmaprice.auth.domain.AppUser;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "uploaded_file")
@Getter
@NoArgsConstructor
public class UploadedFile extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "original_name", nullable = false, length = 255)
	private String originalName;

	@Column(name = "stored_path", nullable = false, length = 500)
	private String storedPath;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private Long sizeBytes;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uploaded_by")
	private AppUser uploadedBy;

	@Builder
	public UploadedFile(String originalName, String storedPath, String contentType, Long sizeBytes, AppUser uploadedBy) {
		this.originalName = originalName;
		this.storedPath = storedPath;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.uploadedBy = uploadedBy;
	}
}
