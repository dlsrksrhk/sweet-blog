package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostSummaryTest {

	@Test
	void 요약을_생성한다() {
		PostSummary summary = new PostSummary("짧은 소개");

		assertThat(summary.value()).isEqualTo("짧은 소개");
	}

	@Test
	void 요약이_null이면_빈_요약으로_생성한다() {
		PostSummary summary = new PostSummary(null);

		assertThat(summary.value()).isEmpty();
	}

	@Test
	void 요약이_blank이면_빈_요약으로_생성한다() {
		PostSummary summary = new PostSummary("   ");

		assertThat(summary.value()).isEmpty();
	}

	@Test
	void 요약은_앞뒤_공백을_제거한다() {
		PostSummary summary = new PostSummary("  짧은 소개  ");

		assertThat(summary.value()).isEqualTo("짧은 소개");
	}

	@Test
	void 요약이_300자를_초과하면_생성할_수_없다() {
		String summary = "a".repeat(301);

		assertThatThrownBy(() -> new PostSummary(summary))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post summary must be 300 characters or less.");
	}

	@Test
	void 요약은_300자까지_허용한다() {
		String summary = "a".repeat(300);

		PostSummary postSummary = new PostSummary(summary);

		assertThat(postSummary.value()).isEqualTo(summary);
	}
}
