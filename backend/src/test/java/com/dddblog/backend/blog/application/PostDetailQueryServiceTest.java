package com.dddblog.backend.blog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

class PostDetailQueryServiceTest {

	private static final Instant TIMESTAMP = Instant.parse("2026-06-27T10:15:30Z");

	@Test
	void 공개된_글_상세를_조회한다() {
		FakePostDetailQueryRepository postDetailQueryRepository = new FakePostDetailQueryRepository();
		PostDetailQueryService postDetailQueryService = new PostDetailQueryService(postDetailQueryRepository);
		PostDetail postDetail = createPostDetail(new PostId(1L));
		postDetailQueryRepository.save(postDetail);

		PostDetail foundPostDetail = postDetailQueryService.getDetail(new PostId(1L));

		assertThat(foundPostDetail).isEqualTo(postDetail);
	}

	@Test
	void 공개된_글이_없으면_조회할_수_없다() {
		FakePostDetailQueryRepository postDetailQueryRepository = new FakePostDetailQueryRepository();
		PostDetailQueryService postDetailQueryService = new PostDetailQueryService(postDetailQueryRepository);

		assertThatThrownBy(() -> postDetailQueryService.getDetail(new PostId(1L)))
			.isInstanceOf(PostNotFoundException.class)
			.hasMessage("Post not found.");
	}

	@Test
	void 조회할_글_ID가_null이면_조회할_수_없다() {
		FakePostDetailQueryRepository postDetailQueryRepository = new FakePostDetailQueryRepository();
		PostDetailQueryService postDetailQueryService = new PostDetailQueryService(postDetailQueryRepository);

		assertThatThrownBy(() -> postDetailQueryService.getDetail(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post id must not be null.");
	}

	private PostDetail createPostDetail(PostId postId) {
		return new PostDetail(
			postId,
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			PostContentType.MARKDOWN,
			new PostContent("# DDD\n\n본문"),
			new PostSummary("DDD 소개"),
			List.of(new TagName("ddd"), new TagName("tdd")),
			PostStatus.PUBLISHED,
			TIMESTAMP,
			TIMESTAMP,
			TIMESTAMP
		);
	}
}
