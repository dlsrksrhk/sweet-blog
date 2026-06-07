package com.dddblog.backend.member.domain;

import java.util.Objects;

public final class LoginId {

	private static final int MIN_LENGTH = 4;
	private static final int MAX_LENGTH = 30;

	private final String value;

	public LoginId(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Login id must not be blank.");
		}
		String trimmedValue = value.trim();
		if (trimmedValue.length() < MIN_LENGTH) {
			throw new IllegalArgumentException("Login id must be 4 characters or more.");
		}
		if (trimmedValue.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Login id must be 30 characters or less.");
		}
		this.value = trimmedValue;
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof LoginId loginId)) {
			return false;
		}
		return Objects.equals(value, loginId.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return value;
	}
}
