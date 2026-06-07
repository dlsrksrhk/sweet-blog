package com.dddblog.backend.blog.persistence;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dddblog.backend.blog.application.PostRepository;
import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.TagName;

@Repository
public class JpaPostRepositoryAdapter implements PostRepository {

	private final SpringDataJpaPostRepository postRepository;
	private final SpringDataJpaTagRepository tagRepository;

	public JpaPostRepositoryAdapter(
		SpringDataJpaPostRepository postRepository,
		SpringDataJpaTagRepository tagRepository
	) {
		this.postRepository = postRepository;
		this.tagRepository = tagRepository;
	}

	@Override
	@Transactional
	public PostId save(Post post) {
		if (post == null) {
			throw new IllegalArgumentException("Post must not be null.");
		}
		Set<JpaTagEntity> tags = findOrCreateTags(post);
		JpaPostEntity entity = new JpaPostEntity(
			post.authorId().value(),
			post.title().value(),
			post.content().value(),
			post.summary().value(),
			post.status(),
			tags
		);
		JpaPostEntity savedEntity = postRepository.save(entity);
		return new PostId(savedEntity.id());
	}

	private Set<JpaTagEntity> findOrCreateTags(Post post) {
		Set<JpaTagEntity> tags = new LinkedHashSet<>();
		for (TagName tagName : post.tags()) {
			JpaTagEntity tag = tagRepository.findByName(tagName.value())
				.orElseGet(() -> tagRepository.save(new JpaTagEntity(tagName.value())));
			tags.add(tag);
		}
		return tags;
	}
}
