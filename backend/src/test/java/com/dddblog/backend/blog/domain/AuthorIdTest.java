package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AuthorIdTest {

	@Test
	void 작성자_ID를_생성한다() {
		AuthorId authorId = new AuthorId(1L);

		assertThat(authorId.value()).isEqualTo(1L);
	}

	@Test
	void 작성자_ID가_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new AuthorId(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Author id must not be null.");
	}
}
