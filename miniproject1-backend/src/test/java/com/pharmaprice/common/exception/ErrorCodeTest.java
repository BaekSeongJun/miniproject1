package com.pharmaprice.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorCodeTest {

	@Test
	void API_문서의_에러코드가_전부_enum에_존재한다() {
		String[] documented = {
			"VALIDATION_FAILED", "INVALID_COORDINATE", "INVALID_DATE_RANGE", "INVALID_RADIUS",
			"UNAUTHENTICATED", "FORBIDDEN", "PHARMACY_NOT_FOUND", "DRUG_NOT_FOUND", "REPORT_NOT_FOUND",
			"EMAIL_ALREADY_EXISTS", "DUPLICATE_REPORT", "FILE_TOO_LARGE", "UNSUPPORTED_FILE_TYPE",
			"DRUG_NOT_OTC", "INTERNAL_ERROR"
		};

		for (String code : documented) {
			assertThat(ErrorCode.valueOf(code)).isNotNull();
		}
	}

	@Test
	void 문서에_없는_코드는_VALIDATION_FAILED로_폴백한다() {
		assertThat(ErrorCode.from("UNKNOWN_CODE")).isEqualTo(ErrorCode.VALIDATION_FAILED);
	}

	@Test
	void INVALID_RADIUS는_400이다() {
		assertThat(ErrorCode.INVALID_RADIUS.status()).isEqualTo(HttpStatus.BAD_REQUEST);
	}
}
