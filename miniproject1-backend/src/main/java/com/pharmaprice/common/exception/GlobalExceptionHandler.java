package com.pharmaprice.common.exception;

import com.pharmaprice.common.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// 서비스 계층은 "CODE: message" 형태의 IllegalArgumentException(400)과
// "CODE" 형태의 NoSuchElementException(404)을 던지는 기존 컨벤션을 따른다 (SearchService, DrugService 등).
// 400 외의 상태코드가 필요하면 ApiException을 던진다.
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// NoSuchElementException(code) 의 code 별 한글 메시지. 목록에 없는 code 는 "대상을 찾을 수 없습니다."로 폴백.
	private static final Map<String, String> NOT_FOUND_MESSAGES = Map.of(
		"PHARMACY_NOT_FOUND", "약국을 찾을 수 없습니다.",
		"DRUG_NOT_FOUND", "약품을 찾을 수 없습니다.",
		"REPORT_NOT_FOUND", "제보를 찾을 수 없습니다.",
		"PRICE_REPORT_NOT_FOUND", "제보를 찾을 수 없습니다.",
		"REGION_NOT_FOUND", "지역을 찾을 수 없습니다.",
		"FILE_NOT_FOUND", "파일을 찾을 수 없습니다.",
		"UPLOADED_FILE_NOT_FOUND", "업로드된 파일을 찾을 수 없습니다."
	);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
		return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
			.map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
			.toList();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of("VALIDATION_FAILED", "요청 값이 유효하지 않습니다.", fieldErrors));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
		String[] parts = ex.getMessage().split(":", 2);
		String code = parts.length > 1 ? parts[0].trim() : "VALIDATION_FAILED";
		String message = parts.length > 1 ? parts[1].trim() : ex.getMessage();
		return ResponseEntity.status(ErrorCode.from(code).status()).body(ErrorResponse.of(code, message));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
		List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
			.map(cv -> new ErrorResponse.FieldError(cv.getPropertyPath().toString(), cv.getMessage()))
			.toList();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of("VALIDATION_FAILED", "요청 값이 유효하지 않습니다.", fieldErrors));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		log.warn("데이터 무결성 제약 위반", ex);
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ErrorResponse.of("CONFLICT", "이미 존재하거나 다른 데이터와 충돌합니다."));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
			.body(ErrorResponse.of("FILE_TOO_LARGE", "파일 크기는 5MB를 초과할 수 없습니다."));
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
		String code = ex.getMessage();
		String message = NOT_FOUND_MESSAGES.getOrDefault(code, "대상을 찾을 수 없습니다.");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(code, message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("처리되지 않은 예외", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.of("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
	}
}
