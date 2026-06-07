package com.dddblog.backend.member.domain;

import java.util.Objects;

public final class Nickname {

	private static final int MIN_LENGTH = 2;
	private static final int MAX_LENGTH = 20;

	private final String value;

	public Nickname(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Nickname must not be blank.");
		}
		String trimmedValue = value.trim();
		if (trimmedValue.length() < MIN_LENGTH) {
			throw new IllegalArgumentException("Nickname must be 2 characters or more.");
		}
		if (trimmedValue.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Nickname must be 20 characters or less.");
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
		if (!(object instanceof Nickname nickname)) {
			return false;
		}
		return Objects.equals(value, nickname.value);
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
