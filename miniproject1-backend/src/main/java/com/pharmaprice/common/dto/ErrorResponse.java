package com.pharmaprice.common.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
	String code, String message, List<FieldError> fieldErrors, String traceId, OffsetDateTime timestamp
) {
	public record FieldError(String field, String reason) {}

	public static ErrorResponse of(String code, String message) {
		return of(code, message, null);
	}

	public static ErrorResponse of(String code, String message, List<FieldError> fieldErrors) {
		return new ErrorResponse(
			code, message, fieldErrors, java.util.UUID.randomUUID().toString().substring(0, 8),
			OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul")));
	}
}
