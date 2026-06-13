package com.dddblog.backend.member.domain;

public record RawPassword(String value) {

	public RawPassword {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Password must not be blank.");
		}
		if (value.length() < 8) {
			throw new IllegalArgumentException("Password must be at least 8 characters.");
		}
	}
}
