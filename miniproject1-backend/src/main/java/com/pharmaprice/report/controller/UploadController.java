package com.pharmaprice.report.controller;

import com.pharmaprice.auth.security.UserPrincipal;
import com.pharmaprice.report.dto.UploadedFileResponse;
import com.pharmaprice.report.service.UploadService;
import com.pharmaprice.report.service.UploadService.LoadedFile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

	private final UploadService uploadService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UploadedFileResponse upload(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam MultipartFile file, @RequestParam String purpose) {
		return uploadService.upload(principal.userId(), file);
	}

	@GetMapping("/{fileId}")
	public ResponseEntity<org.springframework.core.io.Resource> download(
			@AuthenticationPrincipal UserPrincipal principal, @PathVariable long fileId) {
		LoadedFile loaded = uploadService.load(principal.userId(), principal.role(), fileId);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(loaded.contentType()))
			.header(HttpHeaders.CONTENT_DISPOSITION,
				ContentDisposition.inline().filename(loaded.originalName()).build().toString())
			.body(loaded.resource());
	}
}
