package com.pharmaprice.recommendation.controller;

import com.pharmaprice.recommendation.dto.SearchResponse;
import com.pharmaprice.recommendation.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

	private final SearchService searchService;

	@GetMapping
	public SearchResponse search(
			@RequestParam(required = false) Long drugId,
			@RequestParam(required = false) Double lat,
			@RequestParam(required = false) Double lng,
			@RequestParam(required = false) String regionCode,
			@RequestParam(defaultValue = "2000") int radius,
			@RequestParam(defaultValue = "SCORE") String sort,
			@RequestParam(defaultValue = "20") int limit) {
		return searchService.search(drugId, lat, lng, regionCode, radius, sort, limit);
	}
}
