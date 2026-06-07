package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class PostTest {

	@Test
	void 글을_생성한다() {
		AuthorId authorId = new AuthorId(1L);
		PostTitle title = new PostTitle("DDD 시작하기");
		PostContent content = new PostContent("# DDD\n\n본문");
		PostSummary summary = new PostSummary("DDD 소개");
		List<TagName> tags = List.of(new TagName("ddd"), new TagName("tdd"));

		Post post = new Post(authorId, title, content, summary, tags, PostStatus.DRAFT);

		assertThat(post.authorId()).isEqualTo(authorId);
		assertThat(post.title()).isEqualTo(title);
		assertThat(post.content()).isEqualTo(content);
		assertThat(post.summary()).isEqualTo(summary);
		assertThat(post.tags()).containsExactlyElementsOf(tags);
		assertThat(post.status()).isEqualTo(PostStatus.DRAFT);
	}

	@Test
	void 작성자가_없으면_글을_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			null,
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post author must not be null.");
	}

	@Test
	void 제목이_없으면_글을_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			null,
			new PostContent("본문"),
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must not be null.");
	}

	@Test
	void 본문이_없으면_글을_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			null,
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content must not be null.");
	}

	@Test
	void 상태가_없으면_글을_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			List.of(),
			null
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post status must not be null.");
	}

	@Test
	void 요약이_없으면_빈_요약으로_글을_생성한다() {
		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			null,
			List.of(),
			PostStatus.DRAFT
		);

		assertThat(post.summary().value()).isEmpty();
	}

	@Test
	void 태그_목록이_null이면_빈_태그_목록으로_글을_생성한다() {
		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			null,
			PostStatus.DRAFT
		);

		assertThat(post.tags()).isEmpty();
	}

	@Test
	void 태그는_10개까지_허용한다() {
		List<TagName> tags = List.of(
			new TagName("tag1"),
			new TagName("tag2"),
			new TagName("tag3"),
			new TagName("tag4"),
			new TagName("tag5"),
			new TagName("tag6"),
			new TagName("tag7"),
			new TagName("tag8"),
			new TagName("tag9"),
			new TagName("tag10")
		);

		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			tags,
			PostStatus.DRAFT
		);

		assertThat(post.tags()).hasSize(10);
	}

	@Test
	void 태그가_10개를_초과하면_글을_생성할_수_없다() {
		List<TagName> tags = List.of(
			new TagName("tag1"),
			new TagName("tag2"),
			new TagName("tag3"),
			new TagName("tag4"),
			new TagName("tag5"),
			new TagName("tag6"),
			new TagName("tag7"),
			new TagName("tag8"),
			new TagName("tag9"),
			new TagName("tag10"),
			new TagName("tag11")
		);

		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			tags,
			PostStatus.DRAFT
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post tags must be 10 or less.");
	}

	@Test
	void 중복된_태그가_있으면_글을_생성할_수_없다() {
		List<TagName> tags = List.of(new TagName("Java"), new TagName("java"));

		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			tags,
			PostStatus.DRAFT
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post tags must not be duplicated.");
	}

	@Test
	void 태그_목록에_null이_있으면_글을_생성할_수_없다() {
		List<TagName> tags = new java.util.ArrayList<>();
		tags.add(new TagName("ddd"));
		tags.add(null);

		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			tags,
			PostStatus.DRAFT
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post tag must not be null.");
	}

	@Test
	void 외부에_노출된_태그_목록은_수정할_수_없다() {
		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			List.of(new TagName("ddd")),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> post.tags().add(new TagName("tdd")))
			.isInstanceOf(UnsupportedOperationException.class);
	}
}
