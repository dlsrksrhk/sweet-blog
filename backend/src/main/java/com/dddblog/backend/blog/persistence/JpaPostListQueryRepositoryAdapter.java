package com.dddblog.backend.blog.persistence;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dddblog.backend.blog.application.PostListItem;
import com.dddblog.backend.blog.application.PostListPage;
import com.dddblog.backend.blog.application.PostListQuery;
import com.dddblog.backend.blog.application.PostListQueryRepository;
import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

@Repository
public class JpaPostListQueryRepositoryAdapter implements PostListQueryRepository {

	private final SpringDataJpaPostRepository postRepository;

	public JpaPostListQueryRepositoryAdapter(SpringDataJpaPostRepository postRepository) {
		this.postRepository = postRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public PostListPage findPublished(PostListQuery query) {
		PageRequest pageRequest = PageRequest.of(
			query.page(),
			query.size(),
			Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))
		);
		Page<JpaPostEntity> page = postRepository.findByStatus(PostStatus.PUBLISHED, pageRequest);
		return new PostListPage(
			page.getContent().stream()
				.map(this::toPostListItem)
				.toList(),
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages(),
			page.hasNext()
		);
	}

	private PostListItem toPostListItem(JpaPostEntity entity) {
		return new PostListItem(
			new PostId(entity.id()),
			new AuthorId(entity.authorId()),
			new PostTitle(entity.title()),
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
