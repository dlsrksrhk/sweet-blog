package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PasswordHashTest {

	@Test
	void 비밀번호_해시를_생성한다() {
		PasswordHash passwordHash = new PasswordHash("$2a$10$hashed-password");

		assertThat(passwordHash.value()).isEqualTo("$2a$10$hashed-password");
	}

	@Test
	void toString은_비밀번호_해시를_노출하지_않는다() {
		PasswordHash passwordHash = new PasswordHash("$2a$10$hashed-password");

		assertThat(passwordHash.toString()).isEqualTo("[PROTECTED]");
	}

	@Test
	void 비밀번호_해시가_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PasswordHash(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password hash must not be blank.");
	}

	@Test
	void 비밀번호_해시가_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PasswordHash("   "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password hash must not be blank.");
	}
}
