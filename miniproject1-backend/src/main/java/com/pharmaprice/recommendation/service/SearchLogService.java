package com.pharmaprice.recommendation.service;

import com.pharmaprice.recommendation.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchLogService {

	private static final Logger log = LoggerFactory.getLogger(SearchLogService.class);

	private final SearchLogRepository searchLogRepository;

	// 검색 로깅은 부가 기능 - 실패해도 검색 자체는 실패하면 안 된다.
	// SearchService는 readOnly 트랜잭션이라 별도 빈에서 REQUIRES_NEW로 쓴다.
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logSafely(long drugId) {
		try {
			searchLogRepository.insert(drugId);
		} catch (Exception e) {
			log.warn("search log insert failed drugId={}", drugId, e);
		}
	}
}
