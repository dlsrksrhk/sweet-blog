package com.dddblog.backend.blog.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.dddblog.backend.blog.application.PostListItem;
import com.dddblog.backend.blog.application.PostListPage;
import com.dddblog.backend.blog.application.PostListQuery;
import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;
import com.dddblog.backend.support.MysqlDataJpaTestSupport;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaPostRepositoryAdapter.class, JpaPostListQueryRepositoryAdapter.class})
class JpaPostListQueryRepositoryAdapterTest extends MysqlDataJpaTestSupport {

	private static final Instant FIRST = Instant.parse("2026-06-27T10:15:30Z");
	private static final Instant SECOND = Instant.parse("2026-06-27T10:16:30Z");
	private static final Instant THIRD = Instant.parse("2026-06-27T10:17:30Z");

	@Autowired
	private JpaPostRepositoryAdapter postRepository;

	@Autowired
	private JpaPostListQueryRepositoryAdapter postListQueryRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 공개된_글_목록을_조회한다() {
		PostId postId = postRepository.save(createPost("DDD 시작하기", PostStatus.PUBLISHED, FIRST, "ddd", "tdd"));
		entityManager.flush();
		entityManager.clear();

		PostListPage page = postListQueryRepository.findPublished(new PostListQuery(0, 10));

		assertThat(page.items()).hasSize(1);
		PostListItem item = page.items().get(0);
		assertThat(item.postId()).isEqualTo(postId);
		assertThat(item.authorId()).isEqualTo(new AuthorId(1L));
		assertThat(item.title().value()).isEqualTo("DDD 시작하기");
		assertThat(item.summary().value()).isEqualTo("DDD 시작하기 요약");
		assertThat(item.tags()).extracting(TagName::value).containsExactly("ddd", "tdd");
		assertThat(item.status()).isEqualTo(PostStatus.PUBLISHED);
		assertThat(item.createdAt()).isEqualTo(FIRST);
		assertThat(item.updatedAt()).isEqualTo(FIRST);
		assertThat(item.publishedAt()).isEqualTo(FIRST);
		assertThat(page.page()).isZero();
		assertThat(page.size()).isEqualTo(10);
		assertThat(page.totalElements()).isEqualTo(1);
		assertThat(page.totalPages()).isEqualTo(1);
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void 임시_저장_글과_숨김_글은_목록에_포함하지_않는다() {
		PostId publishedId = postRepository.save(createPost("공개 글", PostStatus.PUBLISHED, FIRST));
		postRepository.save(createPost("임시 저장 글", PostStatus.DRAFT, null));
		postRepository.save(createPost("숨김 글", PostStatus.HIDDEN, null));
		entityManager.flush();
		entityManager.clear();

		PostListPage page = postListQueryRepository.findPublished(new PostListQuery(0, 10));

		assertThat(page.items()).extracting(item -> item.postId().value()).containsExactly(publishedId.value());
		assertThat(page.totalElements()).isEqualTo(1);
	}

	@Test
	void 발행일_내림차순으로_조회한다() {
		PostId firstId = postRepository.save(createPost("먼저 발행한 글", PostStatus.PUBLISHED, FIRST));
		PostId secondId = postRepository.save(createPost("나중에 발행한 글", PostStatus.PUBLISHED, SECOND));
		entityManager.flush();
		entityManager.clear();

		PostListPage page = postListQueryRepository.findPublished(new PostListQuery(0, 10));

		assertThat(page.items())
			.extracting(item -> item.postId().value())
			.containsExactly(secondId.value(), firstId.value());
	}

	@Test
	void 발행일이_같으면_글_ID_내림차순으로_조회한다() {
		PostId firstId = postRepository.save(createPost("먼저 저장한 글", PostStatus.PUBLISHED, FIRST));
		PostId secondId = postRepository.save(createPost("나중에 저장한 글", PostStatus.PUBLISHED, FIRST));
		entityManager.flush();
		entityManager.clear();

		PostListPage page = postListQueryRepository.findPublished(new PostListQuery(0, 20));

		assertThat(page.items())
			.extracting(item -> item.postId().value())
			.containsExactly(secondId.value(), firstId.value());
	}

	@Test
	void 페이지와_크기를_적용해_조회한다() {
		PostId firstId = postRepository.save(createPost("첫 번째 글", PostStatus.PUBLISHED, FIRST));
		postRepository.save(createPost("두 번째 글", PostStatus.PUBLISHED, SECOND));
		postRepository.save(createPost("세 번째 글", PostStatus.PUBLISHED, THIRD));
		entityManager.flush();
		entityManager.clear();

		PostListPage page = postListQueryRepository.findPublished(new PostListQuery(1, 2));

		assertThat(page.items()).extracting(item -> item.postId().value()).containsExactly(firstId.value());
		assertThat(page.page()).isEqualTo(1);
		assertThat(page.size()).isEqualTo(2);
		assertThat(page.totalElements()).isEqualTo(3);
		assertThat(page.totalPages()).isEqualTo(2);
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void 태그를_이름_오름차순으로_조회한다() {
		postRepository.save(createPost("태그 정렬 글", PostStatus.PUBLISHED, FIRST, "tdd", "ddd"));
		entityManager.flush();
		entityManager.clear();

		PostListPage page = postListQueryRepository.findPublished(new PostListQuery(0, 10));

		assertThat(page.items().get(0).tags()).extracting(TagName::value).containsExactly("ddd", "tdd");
	}

	private Post createPost(String title, PostStatus status, Instant publishedAt, String... tags) {
		Instant timestamp = publishedAt == null ? FIRST : publishedAt;
		return new Post(
			new AuthorId(1L),
			new PostTitle(title),
			new PostContent("# " + title + "\n\n본문"),
			PostContentType.MARKDOWN,
			new PostSummary(title + " 요약"),
			toTagNames(tags),
			status,
			timestamp,
			timestamp,
			publishedAt
		);
	}

	private List<TagName> toTagNames(String... tags) {
		return List.of(tags).stream()
			.map(TagName::new)
			.toList();
	}
}
