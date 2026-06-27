package com.dddblog.backend.blog.application;

import com.dddblog.backend.blog.domain.PostId;

public class PostDetailQueryService {

	private final PostDetailQueryRepository postDetailQueryRepository;

	public PostDetailQueryService(PostDetailQueryRepository postDetailQueryRepository) {
		this.postDetailQueryRepository = postDetailQueryRepository;
	}

	public PostDetail getDetail(PostId postId) {
		if (postId == null) {
			throw new IllegalArgumentException("Post id must not be null.");
		}
		return postDetailQueryRepository.findPublishedById(postId)
			.orElseThrow(PostNotFoundException::new);
	}
}
