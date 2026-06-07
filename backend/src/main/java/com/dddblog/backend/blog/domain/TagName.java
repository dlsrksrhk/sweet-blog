package com.dddblog.backend.blog.domain;

import java.util.Locale;
import java.util.Objects;

public final class TagName {

	private static final int MAX_LENGTH = 30;

	private final String value;

	public TagName(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Tag name must not be blank.");
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		if (normalized.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Tag name must be 30 characters or less.");
		}
		this.value = normalized;
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof TagName tagName)) {
			return false;
		}
		return Objects.equals(value, tagName.value);
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
