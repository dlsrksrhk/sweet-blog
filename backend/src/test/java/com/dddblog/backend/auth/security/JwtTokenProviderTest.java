package com.dddblog.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

class JwtTokenProviderTest {

	private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha256";
	private static final Instant NOW = Instant.parse("2026-06-17T00:00:00Z");

	@Test
	void 액세스_토큰을_생성하면_회원_ID와_권한을_파싱할_수_있다() {
		JwtTokenProvider tokenProvider = tokenProviderAt(NOW, 3600);

		String token = tokenProvider.createAccessToken(new MemberId(1L), MemberRole.MEMBER);

		ParsedAccessToken parsedToken = tokenProvider.parseAccessToken(token);
		assertThat(parsedToken.memberId()).isEqualTo(new MemberId(1L));
		assertThat(parsedToken.role()).isEqualTo(MemberRole.MEMBER);
	}

	@Test
	void 만료된_토큰이면_검증에_실패한다() {
		JwtTokenProvider issuer = tokenProviderAt(NOW, 1);
		String token = issuer.createAccessToken(new MemberId(1L), MemberRole.MEMBER);
		JwtTokenProvider verifier = tokenProviderAt(NOW.plusSeconds(2), 1);

		assertThatThrownBy(() -> verifier.parseAccessToken(token))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Authentication failed.");
	}

	@Test
	void 위조된_토큰이면_검증에_실패한다() {
		JwtTokenProvider tokenProvider = tokenProviderAt(NOW, 3600);
		String token = tokenProvider.createAccessToken(new MemberId(1L), MemberRole.MEMBER);
		String tamperedToken = token.substring(0, token.length() - 2) + "xx";

		assertThatThrownBy(() -> tokenProvider.parseAccessToken(tamperedToken))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Authentication failed.");
	}

	@Test
	void 액세스_토큰이_아니면_검증에_실패한다() {
		JwtTokenProvider tokenProvider = tokenProviderAt(NOW, 3600);
		String token = tokenProvider.createToken(new MemberId(1L), MemberRole.MEMBER, "refresh");

		assertThatThrownBy(() -> tokenProvider.parseAccessToken(token))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Authentication failed.");
	}

	private JwtTokenProvider tokenProviderAt(Instant now, long validitySeconds) {
		return new JwtTokenProvider(
			new JwtProperties(SECRET, validitySeconds),
			Clock.fixed(now, ZoneOffset.UTC)
		);
	}
}
