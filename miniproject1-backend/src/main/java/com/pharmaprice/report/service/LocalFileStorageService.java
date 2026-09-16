package com.pharmaprice.report.service;

import com.pharmaprice.common.config.UploadProperties;
import com.pharmaprice.common.exception.ApiException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

	private final UploadProperties uploadProperties;

	@Override
	public StoredFile store(MultipartFile file) {
		String contentType = detectImageContentType(file);

		LocalDate today = LocalDate.now();
		Path dir = Path.of(uploadProperties.uploadDir())
			.resolve(String.valueOf(today.getYear()))
			.resolve("%02d".formatted(today.getMonthValue()));
		String ext = switch (contentType) {
			case "image/jpeg" -> "jpg";
			case "image/png" -> "png";
			default -> "webp";
		};
		String filename = UUID.randomUUID() + "." + ext;

		try {
			Files.createDirectories(dir);
			Path target = dir.resolve(filename);
			file.transferTo(target);
			return new StoredFile(dir.resolve(filename).toString(), contentType);
		} catch (IOException e) {
			throw new UncheckedIOException("파일 저장에 실패했습니다.", e);
		}
	}

	@Override
	public Resource loadAsResource(String storedPath) {
		return new FileSystemResource(storedPath);
	}

	// 확장자가 아니라 실제 매직 바이트로 판정한다.
	private String detectImageContentType(MultipartFile file) {
		byte[] header;
		try {
			header = file.getInputStream().readNBytes(12);
		} catch (IOException e) {
			throw new UncheckedIOException("파일을 읽을 수 없습니다.", e);
		}
		if (startsWith(header, 0xFF, 0xD8, 0xFF)) {
			return "image/jpeg";
		}
		if (startsWith(header, 0x89, 0x50, 0x4E, 0x47)) {
			return "image/png";
		}
		if (header.length >= 12 && startsWith(header, 'R', 'I', 'F', 'F') && matchesAt(header, 8, 'W', 'E', 'B', 'P')) {
			return "image/webp";
		}
		throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE_TYPE",
			"jpeg, png, webp 형식만 업로드할 수 있습니다.");
	}

	private boolean startsWith(byte[] header, int... signature) {
		return matchesAt(header, 0, signature);
	}

	private boolean matchesAt(byte[] header, int offset, int... signature) {
		if (header.length < offset + signature.length) {
			return false;
		}
		for (int i = 0; i < signature.length; i++) {
			if ((header[offset + i] & 0xFF) != signature[i]) {
				return false;
			}
		}
		return true;
	}
}
