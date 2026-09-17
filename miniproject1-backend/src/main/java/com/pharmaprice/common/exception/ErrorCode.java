package com.pharmaprice.common.exception;

import org.springframework.http.HttpStatus;

// API.md §1.3 에러 코드 표와 1:1 대응. 표가 바뀌면 여기도 같이 바꾼다.
public enum ErrorCode {
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
	INVALID_COORDINATE(HttpStatus.BAD_REQUEST),
	INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST),
	INVALID_RADIUS(HttpStatus.BAD_REQUEST),
	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
	FORBIDDEN(HttpStatus.FORBIDDEN),
	PHARMACY_NOT_FOUND(HttpStatus.NOT_FOUND),
	DRUG_NOT_FOUND(HttpStatus.NOT_FOUND),
	REPORT_NOT_FOUND(HttpStatus.NOT_FOUND),
	PRICE_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND),
	REGION_NOT_FOUND(HttpStatus.NOT_FOUND),
	FILE_NOT_FOUND(HttpStatus.NOT_FOUND),
	UPLOADED_FILE_NOT_FOUND(HttpStatus.NOT_FOUND),
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),
	DUPLICATE_REPORT(HttpStatus.CONFLICT),
	CONFLICT(HttpStatus.CONFLICT),
	FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),
	UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
	DRUG_NOT_OTC(HttpStatus.UNPROCESSABLE_ENTITY),
	DRUG_PENDING_APPROVAL(HttpStatus.UNPROCESSABLE_ENTITY),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

	private final HttpStatus status;

	ErrorCode(HttpStatus status) {
		this.status = status;
	}

	public HttpStatus status() {
		return status;
	}

	// 서비스 계층이 던지는 "CODE: message" 문자열의 CODE가 문서에 없는 값이면
	// 코드-문서 불일치를 여기서 바로 드러낸다.
	public static ErrorCode from(String code) {
		try {
			return valueOf(code);
		} catch (IllegalArgumentException e) {
			return VALIDATION_FAILED;
		}
	}
}
