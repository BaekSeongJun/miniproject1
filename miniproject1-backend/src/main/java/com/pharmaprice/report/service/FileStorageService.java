package com.pharmaprice.report.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

// S3 전환 시 이 인터페이스의 구현체만 교체한다.
public interface FileStorageService {

	record StoredFile(String storedPath, String contentType) {
	}

	/** 매직 바이트로 이미지 타입을 검증하고 저장한다. 지원하지 않는 타입이면 415 ApiException. */
	StoredFile store(MultipartFile file);

	Resource loadAsResource(String storedPath);
}
