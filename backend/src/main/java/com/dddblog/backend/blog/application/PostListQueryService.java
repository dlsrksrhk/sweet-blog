package com.dddblog.backend.blog.application;

public class PostListQueryService {

	private static final int MAX_PAGE_SIZE = 50;

	private final PostListQueryRepository postListQueryRepository;

	public PostListQueryService(PostListQueryRepository postListQueryRepository) {
		this.postListQueryRepository = postListQueryRepository;
	}

	public PostListPage findPublished(PostListQuery query) {
		validate(query);
		return postListQueryRepository.findPublished(query);
	}

	private void validate(PostListQuery query) {
		if (query == null) {
			throw new IllegalArgumentException("Post list query must not be null.");
		}
		if (query.page() < 0) {
			throw new IllegalArgumentException("Page must be zero or positive.");
		}
		if (query.size() < 1) {
			throw new IllegalArgumentException("Page size must be at least 1.");
		}
		if (query.size() > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Page size must be 50 or less.");
		}
	}
}
