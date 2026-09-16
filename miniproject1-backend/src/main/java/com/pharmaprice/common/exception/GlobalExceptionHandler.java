package com.pharmaprice.common.exception;

import com.pharmaprice.common.dto.ErrorResponse;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 서비스 계층은 "CODE: message" 형태의 IllegalArgumentException(400)과
// "CODE" 형태의 NoSuchElementException(404)을 던지는 기존 컨벤션을 따른다 (SearchService, DrugService 등).
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
		String[] parts = ex.getMessage().split(":", 2);
		String code = parts.length > 1 ? parts[0].trim() : "VALIDATION_FAILED";
		String message = parts.length > 1 ? parts[1].trim() : ex.getMessage();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(code, message));
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(ex.getMessage(), ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("처리되지 않은 예외", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.of("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
	}
}
