package com.pharmaprice.common.dto;

import com.pharmaprice.common.logging.TraceIdFilter;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.MDC;

public record ErrorResponse(
	String code, String message, List<FieldError> fieldErrors, String traceId, OffsetDateTime timestamp
) {
	public record FieldError(String field, String reason) {}

	public static ErrorResponse of(String code, String message) {
		return of(code, message, null);
	}

	public static ErrorResponse of(String code, String message, List<FieldError> fieldErrors) {
		return new ErrorResponse(
			code, message, fieldErrors, MDC.get(TraceIdFilter.TRACE_ID_KEY),
			OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul")));
	}
}
