package com.dddblog.backend.blog.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

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
@Import(JpaPostRepositoryAdapter.class)
class JpaPostRepositoryAdapterTest extends MysqlDataJpaTestSupport {

	private static final Instant TIMESTAMP = Instant.parse("2026-06-27T10:15:30Z");

	@Autowired
	private JpaPostRepositoryAdapter postRepository;

	@Autowired
	private SpringDataJpaPostRepository springDataPostRepository;

	@Autowired
	private SpringDataJpaTagRepository springDataTagRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 글을_저장하면_ID를_반환한다() {
		Post post = createPost();

		PostId postId = postRepository.save(post);

		assertThat(postId.value()).isPositive();
	}

	@Test
	void 글을_저장하면_본문_값이_posts에_저장된다() {
		Post post = createPost();

		PostId postId = postRepository.save(post);

		entityManager.flush();
		entityManager.clear();

		JpaPostEntity savedPost = springDataPostRepository.findById(postId.value()).orElseThrow();
		assertThat(savedPost.authorId()).isEqualTo(1L);
		assertThat(savedPost.title()).isEqualTo("DDD 시작하기");
		assertThat(savedPost.contentType()).isEqualTo(PostContentType.MARKDOWN);
		assertThat(savedPost.contentMarkdown()).isEqualTo("# DDD\n\n본문");
		assertThat(savedPost.summary()).isEqualTo("DDD 소개");
		assertThat(savedPost.status()).isEqualTo(PostStatus.DRAFT);
		assertThat(savedPost.tags()).extracting(JpaTagEntity::name).containsExactlyInAnyOrder("ddd", "tdd");
	}

	@Test
	void 이미_존재하는_태그는_새로_만들지_않고_재사용한다() {
		springDataTagRepository.save(new JpaTagEntity("ddd"));
		Post firstPost = createPost();
		Post secondPost = new Post(
			new AuthorId(2L),
			new PostTitle("JPA 시작하기"),
			new PostContent("JPA 본문"),
			PostContentType.MARKDOWN,
			new PostSummary("JPA 소개"),
			List.of(new TagName("DDD")),
			PostStatus.PUBLISHED,
			TIMESTAMP,
			TIMESTAMP,
			TIMESTAMP
		);

		postRepository.save(firstPost);
		postRepository.save(secondPost);

		assertThat(springDataTagRepository.findAll())
			.extracting(JpaTagEntity::name)
			.containsExactlyInAnyOrder("ddd", "tdd");
	}

	@Test
	void 글이_null이면_저장할_수_없다() {
		assertThatThrownBy(() -> postRepository.save(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post must not be null.");
		assertThat(springDataPostRepository.findAll()).isEmpty();
		assertThat(springDataTagRepository.findAll()).isEmpty();
	}

	private Post createPost() {
		return new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("# DDD\n\n본문"),
			PostContentType.MARKDOWN,
			new PostSummary("DDD 소개"),
			List.of(new TagName("ddd"), new TagName("tdd")),
			PostStatus.DRAFT,
			TIMESTAMP,
			TIMESTAMP,
			null
		);
	}
}
