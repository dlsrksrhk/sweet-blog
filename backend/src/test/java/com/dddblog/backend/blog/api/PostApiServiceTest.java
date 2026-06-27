package com.dddblog.backend.blog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.blog.application.CreatePostService;
import com.dddblog.backend.blog.application.FakePostRepository;
import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.member.domain.MemberId;

class PostApiServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-27T10:15:30Z"), ZoneOffset.UTC);

	@Test
	void 글_작성_요청을_애플리케이션_서비스에_전달한다() {
		FakePostRepository postRepository = new FakePostRepository();
		PostApiService postApiService = new PostApiService(new CreatePostService(postRepository, CLOCK));
		PostRequest request = new PostRequest(
			"DDD 시작하기",
			PostContentType.MARKDOWN,
			"# DDD\n\n본문",
			"DDD 소개",
			List.of("DDD", "TDD"),
			PostStatus.PUBLISHED
		);

		PostResponse response = postApiService.create(new MemberId(1L), request);

		assertThat(response.postId()).isEqualTo(1L);
		Post savedPost = postRepository.savedPosts().get(0);
		assertThat(savedPost.authorId().value()).isEqualTo(1L);
		assertThat(savedPost.title().value()).isEqualTo("DDD 시작하기");
		assertThat(savedPost.contentType()).isEqualTo(PostContentType.MARKDOWN);
		assertThat(savedPost.content().value()).isEqualTo("# DDD\n\n본문");
		assertThat(savedPost.summary().value()).isEqualTo("DDD 소개");
		assertThat(savedPost.tags()).extracting(tag -> tag.value()).containsExactly("ddd", "tdd");
		assertThat(savedPost.status()).isEqualTo(PostStatus.PUBLISHED);
	}

	@Test
	void HTML_본문_형식이면_글을_생성할_수_없다() {
		FakePostRepository postRepository = new FakePostRepository();
		PostApiService postApiService = new PostApiService(new CreatePostService(postRepository, CLOCK));
		PostRequest request = new PostRequest(
			"DDD 시작하기",
			PostContentType.HTML,
			"<p>본문</p>",
			"DDD 소개",
			List.of(),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> postApiService.create(new MemberId(1L), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content type must be MARKDOWN.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}

	@Test
	void 본문_형식이_null이면_글을_생성할_수_없다() {
		FakePostRepository postRepository = new FakePostRepository();
		PostApiService postApiService = new PostApiService(new CreatePostService(postRepository, CLOCK));
		PostRequest request = new PostRequest(
			"DDD 시작하기",
			null,
			"본문",
			"DDD 소개",
			List.of(),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> postApiService.create(new MemberId(1L), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content type must be MARKDOWN.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}
}
