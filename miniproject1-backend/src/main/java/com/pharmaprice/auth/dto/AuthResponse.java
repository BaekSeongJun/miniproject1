package com.pharmaprice.auth.dto;

import com.pharmaprice.auth.domain.AppUser;
import com.pharmaprice.auth.domain.UserRole;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn, UserSummary user) {

	public record UserSummary(Long id, String nickname, UserRole role) {

		public static UserSummary from(AppUser user) {
			return new UserSummary(user.getId(), user.getNickname(), user.getRole());
		}
	}

	public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, AppUser user) {
		return new AuthResponse(accessToken, refreshToken, expiresIn, UserSummary.from(user));
	}
}
