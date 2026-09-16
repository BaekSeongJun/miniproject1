package com.pharmaprice.report.service;

import com.pharmaprice.auth.domain.AppUser;
import com.pharmaprice.auth.domain.UserRole;
import com.pharmaprice.auth.repository.AppUserRepository;
import com.pharmaprice.common.exception.ApiException;
import com.pharmaprice.report.domain.UploadedFile;
import com.pharmaprice.report.dto.UploadedFileResponse;
import com.pharmaprice.report.repository.UploadedFileRepository;
import com.pharmaprice.report.service.FileStorageService.StoredFile;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class UploadService {

	private final FileStorageService fileStorageService;
	private final UploadedFileRepository uploadedFileRepository;
	private final AppUserRepository appUserRepository;

	public UploadedFileResponse upload(long userId, MultipartFile file) {
		StoredFile stored = fileStorageService.store(file);
		AppUser uploader = appUserRepository.getReferenceById(userId);

		UploadedFile uploadedFile = uploadedFileRepository.save(UploadedFile.builder()
			.originalName(file.getOriginalFilename())
			.storedPath(stored.storedPath())
			.contentType(stored.contentType())
			.sizeBytes(file.getSize())
			.uploadedBy(uploader)
			.build());

		return UploadedFileResponse.of(uploadedFile);
	}

	@Transactional(readOnly = true)
	public LoadedFile load(long userId, UserRole role, long fileId) {
		UploadedFile file = uploadedFileRepository.findById(fileId)
			.orElseThrow(() -> new NoSuchElementException("FILE_NOT_FOUND"));
		boolean isOwner = file.getUploadedBy() != null && file.getUploadedBy().getId() == userId;
		if (!isOwner && role != UserRole.ADMIN) {
			throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "본인 또는 관리자만 조회할 수 있습니다.");
		}
		Resource resource = fileStorageService.loadAsResource(file.getStoredPath());
		return new LoadedFile(resource, file.getContentType(), file.getOriginalName());
	}

	public record LoadedFile(Resource resource, String contentType, String originalName) {
	}
}
