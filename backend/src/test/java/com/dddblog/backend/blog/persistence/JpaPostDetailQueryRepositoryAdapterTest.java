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

import com.dddblog.backend.blog.application.PostDetail;
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
@Import({JpaPostRepositoryAdapter.class, JpaPostDetailQueryRepositoryAdapter.class})
class JpaPostDetailQueryRepositoryAdapterTest extends MysqlDataJpaTestSupport {

	private static final Instant TIMESTAMP = Instant.parse("2026-06-27T10:15:30Z");

	@Autowired
	private JpaPostRepositoryAdapter postRepository;

	@Autowired
	private JpaPostDetailQueryRepositoryAdapter postDetailQueryRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 공개된_글을_ID로_조회한다() {
		PostId postId = postRepository.save(createPost(PostStatus.PUBLISHED));
		entityManager.flush();
		entityManager.clear();

		PostDetail postDetail = postDetailQueryRepository.findPublishedById(postId).orElseThrow();

		assertThat(postDetail.postId()).isEqualTo(postId);
		assertThat(postDetail.authorId()).isEqualTo(new AuthorId(1L));
		assertThat(postDetail.title().value()).isEqualTo("DDD 시작하기");
		assertThat(postDetail.contentType()).isEqualTo(PostContentType.MARKDOWN);
		assertThat(postDetail.content().value()).isEqualTo("# DDD\n\n본문");
		assertThat(postDetail.summary().value()).isEqualTo("DDD 소개");
		assertThat(postDetail.status()).isEqualTo(PostStatus.PUBLISHED);
		assertThat(postDetail.createdAt()).isEqualTo(TIMESTAMP);
		assertThat(postDetail.updatedAt()).isEqualTo(TIMESTAMP);
		assertThat(postDetail.publishedAt()).isEqualTo(TIMESTAMP);
		assertThat(postDetail.tags()).extracting(TagName::value).containsExactly("ddd", "tdd");
	}

	@Test
	void 임시_저장_글은_조회하지_않는다() {
		PostId postId = postRepository.save(createPost(PostStatus.DRAFT));
		entityManager.flush();
		entityManager.clear();

		assertThat(postDetailQueryRepository.findPublishedById(postId)).isEmpty();
	}

	@Test
	void 숨김_글은_조회하지_않는다() {
		PostId postId = postRepository.save(createPost(PostStatus.HIDDEN));
		entityManager.flush();
		entityManager.clear();

		assertThat(postDetailQueryRepository.findPublishedById(postId)).isEmpty();
	}

	@Test
	void 태그를_이름_오름차순으로_조회한다() {
		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("# DDD\n\n본문"),
			PostContentType.MARKDOWN,
			new PostSummary("DDD 소개"),
			List.of(new TagName("tdd"), new TagName("ddd")),
			PostStatus.PUBLISHED,
			TIMESTAMP,
			TIMESTAMP,
			TIMESTAMP
		);
		PostId postId = postRepository.save(post);
		entityManager.flush();
		entityManager.clear();

		PostDetail postDetail = postDetailQueryRepository.findPublishedById(postId).orElseThrow();

		assertThat(postDetail.tags()).extracting(TagName::value).containsExactly("ddd", "tdd");
	}

	private Post createPost(PostStatus status) {
		return new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("# DDD\n\n본문"),
			PostContentType.MARKDOWN,
			new PostSummary("DDD 소개"),
			List.of(new TagName("ddd"), new TagName("tdd")),
			status,
			TIMESTAMP,
			TIMESTAMP,
			status == PostStatus.PUBLISHED ? TIMESTAMP : null
		);
	}
}
