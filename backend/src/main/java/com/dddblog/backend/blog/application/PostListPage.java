package com.dddblog.backend.blog.application;

import java.util.List;

public record PostListPage(
	List<PostListItem> items,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {

	public PostListPage {
		if (items == null) {
			items = List.of();
		}
		items = List.copyOf(items);
	}
}
