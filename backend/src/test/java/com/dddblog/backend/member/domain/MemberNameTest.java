package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MemberNameTest {

	@Test
	void 회원_이름을_생성한다() {
		MemberName memberName = new MemberName("홍길동");

		assertThat(memberName.value()).isEqualTo("홍길동");
	}

	@Test
	void 이름은_앞뒤_공백을_제거한다() {
		MemberName memberName = new MemberName(" 홍길동 ");

		assertThat(memberName.value()).isEqualTo("홍길동");
	}

	@Test
	void 이름이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new MemberName(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member name must not be blank.");
	}

	@Test
	void 이름이_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new MemberName("   "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member name must not be blank.");
	}

	@Test
	void 이름이_30자를_초과하면_생성할_수_없다() {
		assertThatThrownBy(() -> new MemberName("가".repeat(31)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member name must be 30 characters or less.");
	}
}
