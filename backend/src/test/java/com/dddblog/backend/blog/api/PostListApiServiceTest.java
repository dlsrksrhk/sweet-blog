package com.dddblog.backend.blog.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.blog.application.FakePostListQueryRepository;
import com.dddblog.backend.blog.application.PostListItem;
import com.dddblog.backend.blog.application.PostListPage;
import com.dddblog.backend.blog.application.PostListQuery;
import com.dddblog.backend.blog.application.PostListQueryService;
import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

class PostListApiServiceTest {

	private static final Instant TIMESTAMP = Instant.parse("2026-06-27T10:15:30Z");

	@Test
	void 공개_글_목록_응답으로_변환한다() {
		FakePostListQueryRepository postListQueryRepository = new FakePostListQueryRepository();
		PostListApiService postListApiService = new PostListApiService(
			new PostListQueryService(postListQueryRepository)
		);
		postListQueryRepository.결과를_준비한다(new PostListPage(
			List.of(createPostListItem(new PostId(10L))),
			1,
			10,
			11,
			2,
			false
		));

		PostListResponse response = postListApiService.getList(1, 10);

		assertThat(response.items()).hasSize(1);
		PostListItemResponse item = response.items().get(0);
		assertThat(item.postId()).isEqualTo(10L);
		assertThat(item.authorId()).isEqualTo(1L);
		assertThat(item.title()).isEqualTo("DDD 시작하기");
		assertThat(item.summary()).isEqualTo("DDD 소개");
		assertThat(item.tags()).containsExactly("ddd", "tdd");
		assertThat(item.status()).isEqualTo(PostStatus.PUBLISHED);
		assertThat(item.createdAt()).isEqualTo(TIMESTAMP);
		assertThat(item.updatedAt()).isEqualTo(TIMESTAMP);
		assertThat(item.publishedAt()).isEqualTo(TIMESTAMP);
		assertThat(response.page()).isEqualTo(1);
		assertThat(response.size()).isEqualTo(10);
		assertThat(response.totalElements()).isEqualTo(11);
		assertThat(response.totalPages()).isEqualTo(2);
		assertThat(response.hasNext()).isFalse();
	}

	@Test
	void 페이지와_크기가_null이면_기본값으로_조회한다() {
		FakePostListQueryRepository postListQueryRepository = new FakePostListQueryRepository();
		PostListApiService postListApiService = new PostListApiService(
			new PostListQueryService(postListQueryRepository)
		);
		postListQueryRepository.결과를_준비한다(new PostListPage(List.of(), 0, 20, 0, 0, false));

		postListApiService.getList(null, null);

		assertThat(postListQueryRepository.마지막_조회_조건()).isEqualTo(new PostListQuery(0, 20));
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
