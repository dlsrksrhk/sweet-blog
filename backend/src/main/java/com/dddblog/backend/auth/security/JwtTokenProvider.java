package com.dddblog.backend.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.dddblog.backend.auth.application.AccessTokenIssuer;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider implements AccessTokenIssuer {

	private static final String ACCESS_TOKEN_TYPE = "access";
	private static final String TOKEN_TYPE_CLAIM = "type";
	private static final String ROLE_CLAIM = "role";

	private final JwtProperties properties;
	private final Clock clock;
	private final SecretKey key;

	public JwtTokenProvider(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public String createAccessToken(MemberId memberId, MemberRole role) {
		return createToken(memberId, role, ACCESS_TOKEN_TYPE);
	}

	String createToken(MemberId memberId, MemberRole role, String tokenType) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plusSeconds(properties.accessTokenValiditySeconds());
		return Jwts.builder()
			.subject(memberId.value().toString())
			.claim(ROLE_CLAIM, role.name())
			.claim(TOKEN_TYPE_CLAIM, tokenType)
			.issuedAt(Date.from(issuedAt))
			.expiration(Date.from(expiresAt))
			.signWith(key)
			.compact();
	}

	public ParsedAccessToken parseAccessToken(String token) {
		try {
			Claims claims = Jwts.parser()
				.verifyWith(key)
				.clock(() -> Date.from(clock.instant()))
				.build()
				.parseSignedClaims(token)
				.getPayload();
			if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
				throw new IllegalArgumentException("Authentication failed.");
			}
			return new ParsedAccessToken(
				new MemberId(Long.valueOf(claims.getSubject())),
				MemberRole.valueOf(claims.get(ROLE_CLAIM, String.class))
			);
		}
		catch (RuntimeException ignored) {
			throw new IllegalArgumentException("Authentication failed.");
		}
	}
}
