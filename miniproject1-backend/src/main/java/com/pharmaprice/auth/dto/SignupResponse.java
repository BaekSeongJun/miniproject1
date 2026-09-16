package com.pharmaprice.auth.dto;

import com.pharmaprice.auth.domain.AppUser;
import com.pharmaprice.auth.domain.UserRole;
import java.time.OffsetDateTime;

public record SignupResponse(Long id, String email, String nickname, UserRole role, OffsetDateTime createdAt) {

	public static SignupResponse from(AppUser user) {
		return new SignupResponse(
			user.getId(), user.getEmail(), user.getNickname(), user.getRole(),
			user.getCreatedAt().atOffset(java.time.ZoneOffset.ofHours(9)));
	}
}
