package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostContentTest {

	@Test
	void 본문을_생성한다() {
		PostContent content = new PostContent("# 제목\n\n본문입니다.");

		assertThat(content.value()).isEqualTo("# 제목\n\n본문입니다.");
	}

	@Test
	void 본문이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostContent(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content must not be blank.");
	}

	@Test
	void 본문이_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostContent("   "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content must not be blank.");
	}
}
