package com.pharmaprice.pharmacy.controller;

import com.pharmaprice.pharmacy.dto.RegionResponse;
import com.pharmaprice.pharmacy.service.RegionService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

	private final RegionService regionService;

	@GetMapping
	public ResponseEntity<List<RegionResponse>> findAll() {
		return ResponseEntity.ok()
			.cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
			.body(regionService.findAllGroupedBySido());
	}
}
