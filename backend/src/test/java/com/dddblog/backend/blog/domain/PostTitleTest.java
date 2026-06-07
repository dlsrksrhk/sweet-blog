package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostTitleTest {

	@Test
	void createsPostTitle() {
		PostTitle title = new PostTitle("DDD and TDD blog");

		assertThat(title.value()).isEqualTo("DDD and TDD blog");
	}

	@Test
	void rejectsNullTitle() {
		assertThatThrownBy(() -> new PostTitle(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must not be blank.");
	}

	@Test
	void rejectsBlankTitle() {
		assertThatThrownBy(() -> new PostTitle("   "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must not be blank.");
	}

	@Test
	void rejectsTitleLongerThanOneHundredCharacters() {
		String title = "a".repeat(101);

		assertThatThrownBy(() -> new PostTitle(title))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must be 100 characters or less.");
	}

	@Test
	void acceptsTitleWithOneHundredCharacters() {
		String title = "a".repeat(100);

		PostTitle postTitle = new PostTitle(title);

		assertThat(postTitle.value()).isEqualTo(title);
	}
}
