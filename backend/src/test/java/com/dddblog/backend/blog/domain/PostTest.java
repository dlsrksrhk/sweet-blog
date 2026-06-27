package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class PostTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-27T10:15:30Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-27T10:15:30Z");
	private static final Instant PUBLISHED_AT = Instant.parse("2026-06-27T10:15:30Z");

	@Test
	void 글을_생성한다() {
		AuthorId authorId = new AuthorId(1L);
		PostTitle title = new PostTitle("DDD 시작하기");
		PostContent content = new PostContent("# DDD\n\n본문");
		PostSummary summary = new PostSummary("DDD 소개");
		List<TagName> tags = List.of(new TagName("ddd"), new TagName("tdd"));

		Post post = new Post(
			authorId,
			title,
			content,
			PostContentType.MARKDOWN,
			summary,
			tags,
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
		);

		assertThat(post.authorId()).isEqualTo(authorId);
		assertThat(post.title()).isEqualTo(title);
		assertThat(post.content()).isEqualTo(content);
		assertThat(post.contentType()).isEqualTo(PostContentType.MARKDOWN);
		assertThat(post.summary()).isEqualTo(summary);
		assertThat(post.tags()).containsExactlyElementsOf(tags);
		assertThat(post.status()).isEqualTo(PostStatus.DRAFT);
		assertThat(post.createdAt()).isEqualTo(CREATED_AT);
		assertThat(post.updatedAt()).isEqualTo(UPDATED_AT);
		assertThat(post.publishedAt()).isNull();
	}

	@Test
	void 공개_글을_생성하면_발행일을_가진다() {
		Post post = createPost(PostStatus.PUBLISHED, CREATED_AT, UPDATED_AT, PUBLISHED_AT);

		assertThat(post.publishedAt()).isEqualTo(PUBLISHED_AT);
	}

	@Test
	void 작성일이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> createPost(PostStatus.DRAFT, null, UPDATED_AT, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post created at must not be null.");
	}

	@Test
	void 수정일이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> createPost(PostStatus.DRAFT, CREATED_AT, null, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post updated at must not be null.");
	}

	@Test
	void 수정일이_작성일보다_이전이면_생성할_수_없다() {
		assertThatThrownBy(() -> createPost(PostStatus.DRAFT, CREATED_AT, CREATED_AT.minusSeconds(1), null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post updated at must not be before created at.");
	}

	@Test
	void 공개_글의_발행일이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> createPost(PostStatus.PUBLISHED, CREATED_AT, UPDATED_AT, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post published at must not be null when published.");
	}

	@Test
	void 발행일이_작성일보다_이전이면_생성할_수_없다() {
		assertThatThrownBy(() -> createPost(PostStatus.PUBLISHED, CREATED_AT, UPDATED_AT, CREATED_AT.minusSeconds(1)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post published at must not be before created at.");
	}

	@Test
	void 작성자가_없으면_글을_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			null,
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
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
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
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
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
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
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			List.of(),
			null,
			CREATED_AT,
			UPDATED_AT,
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
			PostContentType.MARKDOWN,
			null,
			List.of(),
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
		);

		assertThat(post.summary().value()).isEmpty();
	}

	@Test
	void 태그_목록이_null이면_빈_태그_목록으로_글을_생성한다() {
		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			null,
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
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
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			tags,
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
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
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			tags,
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
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
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			tags,
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
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
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			tags,
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
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
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			List.of(new TagName("ddd")),
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
		);

		assertThatThrownBy(() -> post.tags().add(new TagName("tdd")))
			.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void 글을_생성하면_본문_형식을_가진다() {
		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
		);

		assertThat(post.contentType()).isEqualTo(PostContentType.MARKDOWN);
	}

	@Test
	void 본문_형식이_null이면_글을_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			null,
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content type must not be null.");
	}

	private Post createPost(PostStatus status, Instant createdAt, Instant updatedAt, Instant publishedAt) {
		return new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			List.of(),
			status,
			createdAt,
			updatedAt,
			publishedAt
		);
	}
}
