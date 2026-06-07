package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostIdTest {

	@Test
	void 글_ID를_생성한다() {
		PostId postId = new PostId(1L);

		assertThat(postId.value()).isEqualTo(1L);
	}

	@Test
	void 글_ID가_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostId(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post id must not be null.");
	}

	@Test
	void 글_ID가_0이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostId(0L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post id must be positive.");
	}

	@Test
	void 글_ID가_음수이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostId(-1L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post id must be positive.");
	}
}
