package com.dddblog.backend.blog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

class PostListQueryServiceTest {

	private static final Instant TIMESTAMP = Instant.parse("2026-06-27T10:15:30Z");

	@Test
	void 공개_글_목록을_조회한다() {
		FakePostListQueryRepository postListQueryRepository = new FakePostListQueryRepository();
		PostListQueryService postListQueryService = new PostListQueryService(postListQueryRepository);
		PostListQuery query = new PostListQuery(1, 10);
		PostListPage postListPage = new PostListPage(
			List.of(createPostListItem(new PostId(1L))),
			1,
			10,
			11,
			2,
			false
		);
		postListQueryRepository.결과를_준비한다(postListPage);

		PostListPage foundPostListPage = postListQueryService.findPublished(query);

		assertThat(foundPostListPage).isEqualTo(postListPage);
		assertThat(postListQueryRepository.마지막_조회_조건()).isEqualTo(query);
	}

	@Test
	void 조회_조건이_null이면_조회할_수_없다() {
		FakePostListQueryRepository postListQueryRepository = new FakePostListQueryRepository();
		PostListQueryService postListQueryService = new PostListQueryService(postListQueryRepository);

		assertThatThrownBy(() -> postListQueryService.findPublished(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post list query must not be null.");
	}

	@Test
	void 페이지가_음수이면_조회할_수_없다() {
		FakePostListQueryRepository postListQueryRepository = new FakePostListQueryRepository();
		PostListQueryService postListQueryService = new PostListQueryService(postListQueryRepository);

		assertThatThrownBy(() -> postListQueryService.findPublished(new PostListQuery(-1, 10)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Page must be zero or positive.");
	}

	@Test
	void 페이지_크기가_1보다_작으면_조회할_수_없다() {
		FakePostListQueryRepository postListQueryRepository = new FakePostListQueryRepository();
		PostListQueryService postListQueryService = new PostListQueryService(postListQueryRepository);

		assertThatThrownBy(() -> postListQueryService.findPublished(new PostListQuery(0, 0)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Page size must be at least 1.");
	}

	@Test
	void 페이지_크기가_50보다_크면_조회할_수_없다() {
		FakePostListQueryRepository postListQueryRepository = new FakePostListQueryRepository();
		PostListQueryService postListQueryService = new PostListQueryService(postListQueryRepository);

		assertThatThrownBy(() -> postListQueryService.findPublished(new PostListQuery(0, 51)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Page size must be 50 or less.");
	}

	private PostListItem createPostListItem(PostId postId) {
		return new PostListItem(
			postId,
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostSummary("DDD 소개"),
			List.of(new TagName("ddd"), new TagName("tdd")),
			PostStatus.PUBLISHED,
			TIMESTAMP,
			TIMESTAMP,
			TIMESTAMP
		);
	}
}
