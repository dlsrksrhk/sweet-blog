package com.dddblog.backend.blog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.blog.application.FakePostDetailQueryRepository;
import com.dddblog.backend.blog.application.PostDetail;
import com.dddblog.backend.blog.application.PostDetailQueryService;
import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

class PostDetailApiServiceTest {

	@Test
	void 공개된_글_상세를_응답으로_변환한다() {
		FakePostDetailQueryRepository postDetailQueryRepository = new FakePostDetailQueryRepository();
		PostDetailApiService postDetailApiService = new PostDetailApiService(
			new PostDetailQueryService(postDetailQueryRepository)
		);
		postDetailQueryRepository.save(new PostDetail(
			new PostId(10L),
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			PostContentType.MARKDOWN,
			new PostContent("# DDD\n\n본문"),
			new PostSummary("DDD 소개"),
			List.of(new TagName("ddd"), new TagName("tdd")),
			PostStatus.PUBLISHED
		));

		PostDetailResponse response = postDetailApiService.getDetail(10L);

		assertThat(response.postId()).isEqualTo(10L);
		assertThat(response.authorId()).isEqualTo(1L);
		assertThat(response.title()).isEqualTo("DDD 시작하기");
		assertThat(response.contentType()).isEqualTo(PostContentType.MARKDOWN);
		assertThat(response.content()).isEqualTo("# DDD\n\n본문");
		assertThat(response.summary()).isEqualTo("DDD 소개");
		assertThat(response.tags()).containsExactly("ddd", "tdd");
		assertThat(response.status()).isEqualTo(PostStatus.PUBLISHED);
	}

	@Test
	void 글_ID가_유효하지_않으면_조회할_수_없다() {
		FakePostDetailQueryRepository postDetailQueryRepository = new FakePostDetailQueryRepository();
		PostDetailApiService postDetailApiService = new PostDetailApiService(
			new PostDetailQueryService(postDetailQueryRepository)
		);

		assertThatThrownBy(() -> postDetailApiService.getDetail(0L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post id must be positive.");
	}
}
