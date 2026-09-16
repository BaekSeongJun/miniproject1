package com.pharmaprice.auth.security;

import com.pharmaprice.auth.domain.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private static final String CLAIM_TYPE = "type";
	private static final String CLAIM_UID = "uid";
	private static final String CLAIM_ROLE = "role";
	private static final String TYPE_ACCESS = "access";
	private static final String TYPE_REFRESH = "refresh";

	private final SecretKey key;
	private final long accessTokenValiditySeconds;
	private final long refreshTokenValiditySeconds;

	public JwtTokenProvider(
			@Value("${jwt.secret}") String secret,
			@Value("${jwt.access-token-validity-seconds}") long accessTokenValiditySeconds,
			@Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenValiditySeconds = accessTokenValiditySeconds;
		this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
	}

	public String createAccessToken(AppUser user) {
		return createAccessToken(user, Duration.ofSeconds(accessTokenValiditySeconds));
	}

	public String createAccessToken(AppUser user, Duration validity) {
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(user.getEmail())
			.claim(CLAIM_UID, user.getId())
			.claim(CLAIM_ROLE, user.getRole().name())
			.claim(CLAIM_TYPE, TYPE_ACCESS)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(validity)))
			.signWith(key, Jwts.SIG.HS256)
			.compact();
	}

	public String createRefreshToken(AppUser user) {
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(user.getEmail())
			.claim(CLAIM_TYPE, TYPE_REFRESH)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plusSeconds(refreshTokenValiditySeconds)))
			.signWith(key, Jwts.SIG.HS256)
			.compact();
	}

	public Claims parse(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}

	public boolean isValidAccessToken(String token) {
		try {
			Claims claims = parse(token);
			return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public String getEmail(String token) {
		return parse(token).getSubject();
	}

	public static String hashRefreshToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
		}
	}
}
