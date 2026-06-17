package com.dddblog.backend.auth.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JwtAuthentication extends AbstractAuthenticationToken {

	private final AuthenticatedMember principal;

	public JwtAuthentication(AuthenticatedMember principal) {
		super(authorities(principal));
		this.principal = principal;
		setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return "";
	}

	@Override
	public AuthenticatedMember getPrincipal() {
		return principal;
	}

	private static Collection<? extends GrantedAuthority> authorities(AuthenticatedMember principal) {
		return List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
	}
}
