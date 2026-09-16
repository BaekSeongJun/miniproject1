package com.pharmaprice.auth.dto;

import com.pharmaprice.auth.domain.AppUser;
import com.pharmaprice.auth.domain.UserRole;
import java.time.OffsetDateTime;

public record MeResponse(
	Long id, String email, String nickname, UserRole role, Integer reportCount, OffsetDateTime createdAt
) {

	public static MeResponse from(AppUser user) {
		return new MeResponse(
			user.getId(), user.getEmail(), user.getNickname(), user.getRole(), user.getReportCount(),
			user.getCreatedAt().atOffset(java.time.ZoneOffset.ofHours(9)));
	}
}
