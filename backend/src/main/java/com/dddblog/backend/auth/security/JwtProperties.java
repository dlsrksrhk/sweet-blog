package com.dddblog.backend.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long accessTokenValiditySeconds) {

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("JWT secret must not be blank.");
		}
		if (accessTokenValiditySeconds <= 0) {
			throw new IllegalArgumentException("JWT access token validity seconds must be positive.");
		}
	}
}
