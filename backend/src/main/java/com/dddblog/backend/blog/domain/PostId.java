package com.dddblog.backend.blog.domain;

import java.util.Objects;

public final class PostId {

	private final Long value;

	public PostId(Long value) {
		if (value == null) {
			throw new IllegalArgumentException("Post id must not be null.");
		}
		if (value < 1) {
			throw new IllegalArgumentException("Post id must be positive.");
		}
		this.value = value;
	}

	public Long value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof PostId postId)) {
			return false;
		}
		return Objects.equals(value, postId.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
