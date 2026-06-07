package com.dddblog.backend.blog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;

class CreatePostServiceTest {

	@Test
	void 유효한_요청이면_글을_저장하고_ID를_반환한다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"# DDD\n\n본문",
			"DDD 소개",
			List.of("ddd", "tdd"),
			PostStatus.DRAFT
		);

		PostId postId = service.create(command);

		assertThat(postId).isEqualTo(new PostId(1L));
		assertThat(postRepository.savedPosts()).hasSize(1);
	}

	@Test
	void 저장된_글은_요청_값을_도메인_값으로_가진다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"# DDD\n\n본문",
			"DDD 소개",
			List.of("DDD", "TDD"),
			PostStatus.PUBLISHED
		);

		service.create(command);

		Post savedPost = postRepository.savedPosts().get(0);
		assertThat(savedPost.authorId().value()).isEqualTo(1L);
		assertThat(savedPost.title().value()).isEqualTo("DDD 시작하기");
		assertThat(savedPost.content().value()).isEqualTo("# DDD\n\n본문");
		assertThat(savedPost.summary().value()).isEqualTo("DDD 소개");
		assertThat(savedPost.tags()).extracting(tag -> tag.value()).containsExactly("ddd", "tdd");
		assertThat(savedPost.status()).isEqualTo(PostStatus.PUBLISHED);
	}

	@Test
	void 요약이_null이면_빈_요약으로_저장한다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"본문",
			null,
			List.of(),
			PostStatus.DRAFT
		);

		service.create(command);

		assertThat(postRepository.savedPosts().get(0).summary().value()).isEmpty();
	}

	@Test
	void 태그_목록이_null이면_빈_태그_목록으로_저장한다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"본문",
			"요약",
			null,
			PostStatus.DRAFT
		);

		service.create(command);

		assertThat(postRepository.savedPosts().get(0).tags()).isEmpty();
	}

	@Test
	void 태그가_10개를_초과하면_저장하지_않는다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"본문",
			"요약",
			List.of("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tag10", "tag11"),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> service.create(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post tags must be 10 or less.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}

	@Test
	void 중복된_태그가_있으면_저장하지_않는다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"본문",
			"요약",
			List.of("Java", "java"),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> service.create(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post tags must not be duplicated.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}

	@Test
	void 제목이_blank이면_저장하지_않는다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"   ",
			"본문",
			"요약",
			List.of(),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> service.create(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must not be blank.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}

	@Test
	void 본문이_blank이면_저장하지_않는다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"   ",
			"요약",
			List.of(),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> service.create(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content must not be blank.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}

	@Test
	void command가_null이면_저장하지_않는다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);

		assertThatThrownBy(() -> service.create(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Create post command must not be null.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}
}
