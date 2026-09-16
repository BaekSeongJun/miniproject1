package com.pharmaprice.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void 업로드_크기_초과_예외는_413_FILE_TOO_LARGE로_변환된다() {
		ResponseEntity<com.pharmaprice.common.dto.ErrorResponse> response =
			handler.handleMaxUploadSize(new MaxUploadSizeExceededException(5_000_000));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
		assertThat(response.getBody().code()).isEqualTo("FILE_TOO_LARGE");
	}
}
