package com.dddblog.backend.blog.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dddblog.backend.blog.application.PostDetail;
import com.dddblog.backend.blog.application.PostDetailQueryRepository;
import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

@Repository
public class JpaPostDetailQueryRepositoryAdapter implements PostDetailQueryRepository {

	private final SpringDataJpaPostRepository postRepository;

	public JpaPostDetailQueryRepositoryAdapter(SpringDataJpaPostRepository postRepository) {
		this.postRepository = postRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<PostDetail> findPublishedById(PostId postId) {
		return postRepository.findByIdAndStatus(postId.value(), PostStatus.PUBLISHED)
			.map(this::toPostDetail);
	}

	private PostDetail toPostDetail(JpaPostEntity entity) {
		return new PostDetail(
			new PostId(entity.id()),
			new AuthorId(entity.authorId()),
			new PostTitle(entity.title()),
			entity.contentType(),
			new PostContent(entity.contentMarkdown()),
			new PostSummary(entity.summary()),
			toTagNames(entity),
			entity.status(),
			entity.createdAt(),
			entity.updatedAt(),
			entity.publishedAt()
		);
	}

	private List<TagName> toTagNames(JpaPostEntity entity) {
		return entity.tags().stream()
			.map(JpaTagEntity::name)
			.sorted(Comparator.naturalOrder())
			.map(TagName::new)
			.toList();
	}
}
