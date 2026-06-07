package com.dddblog.backend.blog.domain;

import java.util.Objects;

public final class PostSummary {

	private static final int MAX_LENGTH = 300;

	private final String value;

	public PostSummary(String value) {
		String normalized = normalize(value);
		if (normalized.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Post summary must be 300 characters or less.");
		}
		this.value = normalized;
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		return value.trim();
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof PostSummary postSummary)) {
			return false;
		}
		return Objects.equals(value, postSummary.value);
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
