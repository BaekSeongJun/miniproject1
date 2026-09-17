package com.pharmaprice.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmaprice.common.logging.TraceIdFilter;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void 업로드_크기_초과_예외는_413_FILE_TOO_LARGE로_변환된다() {
		ResponseEntity<com.pharmaprice.common.dto.ErrorResponse> response =
			handler.handleMaxUploadSize(new MaxUploadSizeExceededException(5_000_000));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
		assertThat(response.getBody().code()).isEqualTo("FILE_TOO_LARGE");
	}

	@Test
	void 응답의_traceId는_MDC에_심어둔_값과_동일하다() {
		MDC.put(TraceIdFilter.TRACE_ID_KEY, "abc12345");

		ResponseEntity<com.pharmaprice.common.dto.ErrorResponse> response =
			handler.handleUnexpected(new RuntimeException("boom"));

		assertThat(response.getBody().traceId()).isEqualTo("abc12345");
	}

	@Test
	void 문서에_없는_코드로_찾을수없음_예외를_던지면_기본_한글_메시지로_폴백한다() {
		ResponseEntity<com.pharmaprice.common.dto.ErrorResponse> response =
			handler.handleNotFound(new NoSuchElementException("PHARMACY_NOT_FOUND"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().message()).isEqualTo("약국을 찾을 수 없습니다.");
	}

	@Test
	void IllegalArgumentException의_코드에_맞는_상태코드로_응답한다() {
		ResponseEntity<com.pharmaprice.common.dto.ErrorResponse> response =
			handler.handleIllegalArgument(new IllegalArgumentException("INVALID_RADIUS: 100"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().code()).isEqualTo("INVALID_RADIUS");
	}
}
