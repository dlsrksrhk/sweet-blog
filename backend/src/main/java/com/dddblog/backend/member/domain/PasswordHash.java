package com.dddblog.backend.member.domain;

import java.util.Objects;

public final class PasswordHash {

	private final String value;

	public PasswordHash(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Password hash must not be blank.");
		}
		this.value = value;
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof PasswordHash passwordHash)) {
			return false;
		}
		return Objects.equals(value, passwordHash.value);
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
