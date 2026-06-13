package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RawPasswordTest {

	@Test
	void 비밀번호가_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new RawPassword(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password must not be blank.");
	}

	@Test
	void 비밀번호가_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new RawPassword(" "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password must not be blank.");
	}

	@Test
	void 비밀번호가_8자_미만이면_생성할_수_없다() {
		assertThatThrownBy(() -> new RawPassword("1234567"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password must be at least 8 characters.");
	}

	@Test
	void 유효한_비밀번호이면_원문_값을_반환한다() {
		RawPassword password = new RawPassword("password123");

		assertThat(password.value()).isEqualTo("password123");
	}

	@Test
	void toString은_원문_비밀번호를_노출하지_않는다() {
		RawPassword password = new RawPassword("password123");

		assertThat(password.toString()).isEqualTo("[PROTECTED]");
	}
}
