package com.pharmaprice.auth.service;

import com.pharmaprice.auth.domain.AppUser;
import com.pharmaprice.auth.domain.RefreshToken;
import com.pharmaprice.auth.domain.UserRole;
import com.pharmaprice.auth.domain.UserStatus;
import com.pharmaprice.auth.dto.AuthResponse;
import com.pharmaprice.auth.dto.LoginRequest;
import com.pharmaprice.auth.dto.MeResponse;
import com.pharmaprice.auth.dto.SignupRequest;
import com.pharmaprice.auth.dto.SignupResponse;
import com.pharmaprice.auth.repository.AppUserRepository;
import com.pharmaprice.auth.repository.RefreshTokenRepository;
import com.pharmaprice.auth.security.JwtTokenProvider;
import com.pharmaprice.common.exception.ApiException;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final AppUserRepository appUserRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	@Value("${jwt.access-token-validity-seconds}")
	private long accessTokenValiditySeconds;

	@Value("${jwt.refresh-token-validity-seconds}")
	private long refreshTokenValiditySeconds;

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		if (appUserRepository.findByEmail(request.email()).isPresent()) {
			throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");
		}
		AppUser user = AppUser.builder()
			.email(request.email())
			.passwordHash(passwordEncoder.encode(request.password()))
			.nickname(request.nickname())
			.role(UserRole.USER)
			.status(UserStatus.ACTIVE)
			.reportCount(0)
			.build();
		return SignupResponse.from(appUserRepository.save(user));
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		AppUser user = appUserRepository.findByEmail(request.email())
			.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "이메일 또는 비밀번호가 올바르지 않습니다."));
		if (user.getStatus() == UserStatus.SUSPENDED) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "정지된 계정입니다.");
		}
		return issueTokens(user);
	}

	@Transactional
	public AuthResponse refresh(String rawToken) {
		String tokenHash = JwtTokenProvider.hashRefreshToken(rawToken);
		RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
			.filter(t -> t.getRevokedAt() == null && t.getExpiresAt().isAfter(Instant.now()))
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "유효하지 않은 refresh 토큰입니다."));
		token.revoke();
		return issueTokens(token.getUser());
	}

	@Transactional
	public void logout(String rawToken) {
		String tokenHash = JwtTokenProvider.hashRefreshToken(rawToken);
		refreshTokenRepository.findByTokenHash(tokenHash)
			.filter(t -> t.getRevokedAt() == null)
			.ifPresent(RefreshToken::revoke);
	}

	public MeResponse me(Long userId) {
		AppUser user = appUserRepository.findById(userId).orElseThrow(NoSuchElementException::new);
		return MeResponse.from(user);
	}

	private AuthResponse issueTokens(AppUser user) {
		String accessToken = jwtTokenProvider.createAccessToken(user);
		String rawRefreshToken = jwtTokenProvider.createRefreshToken(user);
		refreshTokenRepository.save(RefreshToken.builder()
			.user(user)
			.tokenHash(JwtTokenProvider.hashRefreshToken(rawRefreshToken))
			.expiresAt(Instant.now().plusSeconds(refreshTokenValiditySeconds))
			.build());
		return AuthResponse.of(accessToken, rawRefreshToken, accessTokenValiditySeconds, user);
	}
}
