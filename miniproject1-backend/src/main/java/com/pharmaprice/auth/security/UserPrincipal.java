package com.pharmaprice.auth.security;

import com.pharmaprice.auth.domain.AppUser;
import com.pharmaprice.auth.domain.UserRole;

public record UserPrincipal(Long userId, String email, UserRole role) {

	public static UserPrincipal from(AppUser user) {
		return new UserPrincipal(user.getId(), user.getEmail(), user.getRole());
	}
}
