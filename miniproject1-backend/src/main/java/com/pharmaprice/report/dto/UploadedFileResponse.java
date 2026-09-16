package com.pharmaprice.report.dto;

import com.pharmaprice.report.domain.UploadedFile;
import java.time.Instant;

public record UploadedFileResponse(
	Long id, String originalName, String contentType, Long sizeBytes, String url, Instant createdAt
) {
	public static UploadedFileResponse of(UploadedFile file) {
		return new UploadedFileResponse(
			file.getId(), file.getOriginalName(), file.getContentType(), file.getSizeBytes(),
			"/api/v1/uploads/" + file.getId(), file.getCreatedAt());
	}
}
