package com.dddblog.backend.blog.api;

import java.util.List;

public record PostListResponse(
	List<PostListItemResponse> items,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {
}
