package com.dddblog.backend.blog.domain;

import java.util.Objects;

public final class AuthorId {

	private final Long value;

	public AuthorId(Long value) {
		if (value == null) {
			throw new IllegalArgumentException("Author id must not be null.");
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
		if (!(object instanceof AuthorId authorId)) {
			return false;
		}
		return Objects.equals(value, authorId.value);
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
