package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LoginIdTest {

	@Test
	void 로그인_ID를_생성한다() {
		LoginId loginId = new LoginId("user01");

		assertThat(loginId.value()).isEqualTo("user01");
	}

	@Test
	void 로그인_ID는_앞뒤_공백을_제거한다() {
		LoginId loginId = new LoginId(" user01 ");

		assertThat(loginId.value()).isEqualTo("user01");
	}

	@Test
	void 로그인_ID가_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new LoginId(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id must not be blank.");
	}

	@Test
	void 로그인_ID가_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new LoginId("   "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id must not be blank.");
	}

	@Test
	void 로그인_ID가_4자보다_짧으면_생성할_수_없다() {
		assertThatThrownBy(() -> new LoginId("abc"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id must be 4 characters or more.");
	}

	@Test
	void 로그인_ID가_30자를_초과하면_생성할_수_없다() {
		assertThatThrownBy(() -> new LoginId("a".repeat(31)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id must be 30 characters or less.");
	}
}
