package com.dddblog.backend.member.domain;

import java.util.Objects;

public final class MemberName {

	private static final int MAX_LENGTH = 30;

	private final String value;

	public MemberName(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Member name must not be blank.");
		}
		String trimmedValue = value.trim();
		if (trimmedValue.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Member name must be 30 characters or less.");
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
		if (!(object instanceof MemberName memberName)) {
			return false;
		}
		return Objects.equals(value, memberName.value);
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
