package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TagNameTest {

	@Test
	void 태그명을_생성한다() {
		TagName tagName = new TagName("ddd");

		assertThat(tagName.value()).isEqualTo("ddd");
	}

	@Test
	void 태그명은_앞뒤_공백을_제거하고_소문자로_정규화한다() {
		TagName tagName = new TagName("  Java  ");

		assertThat(tagName.value()).isEqualTo("java");
	}

	@Test
	void 태그명이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new TagName(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Tag name must not be blank.");
	}

	@Test
	void 태그명이_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new TagName("   "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Tag name must not be blank.");
	}

	@Test
	void 태그명이_30자를_초과하면_생성할_수_없다() {
		String tagName = "a".repeat(31);

		assertThatThrownBy(() -> new TagName(tagName))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Tag name must be 30 characters or less.");
	}

	@Test
	void 태그명은_30자까지_허용한다() {
		String tagName = "a".repeat(30);

		TagName created = new TagName(tagName);

		assertThat(created.value()).isEqualTo(tagName);
	}
}
