package com.dddblog.backend.blog.domain;

import java.util.Objects;

public final class PostTitle {

	private static final int MAX_LENGTH = 100;

	private final String value;

	public PostTitle(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Post title must not be blank.");
		}
		if (value.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Post title must be 100 characters or less.");
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
		if (!(object instanceof PostTitle postTitle)) {
			return false;
		}
		return Objects.equals(value, postTitle.value);
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
