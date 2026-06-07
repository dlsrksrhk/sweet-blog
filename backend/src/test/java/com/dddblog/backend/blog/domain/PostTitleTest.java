package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostTitleTest {

	@Test
	void 제목을_생성한다() {
		PostTitle title = new PostTitle("DDD and TDD blog");

		assertThat(title.value()).isEqualTo("DDD and TDD blog");
	}

	@Test
	void 제목이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostTitle(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must not be blank.");
	}

	@Test
	void 제목이_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostTitle("   "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must not be blank.");
	}

	@Test
	void 제목이_100자를_초과하면_생성할_수_없다() {
		String title = "a".repeat(101);

		assertThatThrownBy(() -> new PostTitle(title))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must be 100 characters or less.");
	}

	@Test
	void 제목은_100자까지_허용한다() {
		String title = "a".repeat(100);

		PostTitle postTitle = new PostTitle(title);

		assertThat(postTitle.value()).isEqualTo(title);
	}
}
