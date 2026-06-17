package com.dddblog.backend.auth.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider tokenProvider;
	private final JwtAuthenticationEntryPoint authenticationEntryPoint;

	public JwtAuthenticationFilter(
		JwtTokenProvider tokenProvider,
		JwtAuthenticationEntryPoint authenticationEntryPoint
	) {
		this.tokenProvider = tokenProvider;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			String token = authorization.substring(BEARER_PREFIX.length());
			ParsedAccessToken parsedToken = tokenProvider.parseAccessToken(token);
			AuthenticatedMember authenticatedMember = new AuthenticatedMember(parsedToken.memberId(), parsedToken.role());
			SecurityContextHolder.getContext().setAuthentication(new JwtAuthentication(authenticatedMember));
			filterChain.doFilter(request, response);
		}
		catch (IllegalArgumentException exception) {
			SecurityContextHolder.clearContext();
			authenticationEntryPoint.commence(request, response, null);
		}
	}
}
