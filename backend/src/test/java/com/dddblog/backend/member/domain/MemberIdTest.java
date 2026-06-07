package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MemberIdTest {

	@Test
	void 회원_ID를_생성한다() {
		MemberId memberId = new MemberId(1L);

		assertThat(memberId.value()).isEqualTo(1L);
	}

	@Test
	void 회원_ID가_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new MemberId(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member id must not be null.");
	}

	@Test
	void 회원_ID가_0이면_생성할_수_없다() {
		assertThatThrownBy(() -> new MemberId(0L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member id must be positive.");
	}

	@Test
	void 회원_ID가_음수이면_생성할_수_없다() {
		assertThatThrownBy(() -> new MemberId(-1L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member id must be positive.");
	}
}
