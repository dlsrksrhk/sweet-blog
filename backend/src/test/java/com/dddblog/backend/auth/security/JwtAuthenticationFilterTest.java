package com.dddblog.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtTokenProvider tokenProvider;

	@Mock
	private JwtAuthenticationEntryPoint authenticationEntryPoint;

	@Mock
	private FilterChain filterChain;

	private JwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
		filter = new JwtAuthenticationFilter(tokenProvider, authenticationEntryPoint);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void 유효한_Bearer_토큰이면_JWT_인증을_설정하고_필터_체인을_계속한다() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
		when(tokenProvider.parseAccessToken("valid-token"))
			.thenReturn(new ParsedAccessToken(new MemberId(1L), MemberRole.MEMBER));

		filter.doFilter(request, response, filterChain);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isInstanceOf(JwtAuthentication.class);
		AuthenticatedMember principal = (AuthenticatedMember) authentication.getPrincipal();
		assertThat(principal.memberId()).isEqualTo(new MemberId(1L));
		assertThat(principal.role()).isEqualTo(MemberRole.MEMBER);
		verify(filterChain).doFilter(request, response);
		verify(authenticationEntryPoint, never()).commence(any(), any(), any());
	}

	@Test
	void 유효하지_않은_Bearer_토큰이면_인증을_지우고_진입점을_호출하며_필터_체인을_계속하지_않는다() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
		when(tokenProvider.parseAccessToken("invalid-token"))
			.thenThrow(new IllegalArgumentException("Authentication failed."));

		filter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(authenticationEntryPoint).commence(eq(request), eq(response), isNull());
		verify(filterChain, never()).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
	}

	@Test
	void Authorization_헤더가_없으면_인증_없이_필터_체인을_계속한다() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(tokenProvider, authenticationEntryPoint);
	}

	@Test
	void Bearer_토큰이_아닌_Authorization_헤더이면_인증_없이_필터_체인을_계속한다() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Basic credentials");

		filter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(tokenProvider, authenticationEntryPoint);
	}
}
