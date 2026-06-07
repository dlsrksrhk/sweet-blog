package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NicknameTest {

	@Test
	void 닉네임을_생성한다() {
		Nickname nickname = new Nickname("길동");

		assertThat(nickname.value()).isEqualTo("길동");
	}

	@Test
	void 닉네임은_앞뒤_공백을_제거한다() {
		Nickname nickname = new Nickname(" 길동 ");

		assertThat(nickname.value()).isEqualTo("길동");
	}

	@Test
	void 닉네임이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new Nickname(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname must not be blank.");
	}

	@Test
	void 닉네임이_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new Nickname("   "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname must not be blank.");
	}

	@Test
	void 닉네임이_2자보다_짧으면_생성할_수_없다() {
		assertThatThrownBy(() -> new Nickname("가"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname must be 2 characters or more.");
	}

	@Test
	void 닉네임이_20자를_초과하면_생성할_수_없다() {
		assertThatThrownBy(() -> new Nickname("가".repeat(21)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname must be 20 characters or less.");
	}
}
