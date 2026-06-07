package com.dddblog.backend.blog.domain;

import java.util.Objects;

public final class PostContent {

	private final String value;

	public PostContent(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Post content must not be blank.");
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
		if (!(object instanceof PostContent postContent)) {
			return false;
		}
		return Objects.equals(value, postContent.value);
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
