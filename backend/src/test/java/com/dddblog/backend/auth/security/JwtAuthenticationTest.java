package com.dddblog.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

class JwtAuthenticationTest {

	@Test
	void 회원_역할을_ROLE_권한으로_인증한다() {
		AuthenticatedMember principal = new AuthenticatedMember(new MemberId(1L), MemberRole.MEMBER);

		JwtAuthentication authentication = new JwtAuthentication(principal);

		assertThat(authentication.isAuthenticated()).isTrue();
		assertThat(authentication.getPrincipal()).isEqualTo(principal);
		assertThat(authentication.getAuthorities())
			.containsExactly(new SimpleGrantedAuthority("ROLE_MEMBER"));
	}
}
