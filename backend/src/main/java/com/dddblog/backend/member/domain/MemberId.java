package com.dddblog.backend.member.domain;

import java.util.Objects;

public final class MemberId {

	private final Long value;

	public MemberId(Long value) {
		if (value == null) {
			throw new IllegalArgumentException("Member id must not be null.");
		}
		if (value < 1) {
			throw new IllegalArgumentException("Member id must be positive.");
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
		if (!(object instanceof MemberId memberId)) {
			return false;
		}
		return Objects.equals(value, memberId.value);
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
