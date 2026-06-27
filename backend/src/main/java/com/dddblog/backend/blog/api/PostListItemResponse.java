package com.dddblog.backend.blog.api;

import java.time.Instant;
import java.util.List;

import com.dddblog.backend.blog.domain.PostStatus;

public record PostListItemResponse(
	Long postId,
	Long authorId,
	String title,
	String summary,
	List<String> tags,
	PostStatus status,
	Instant createdAt,
	Instant updatedAt,
	Instant publishedAt
) {
}
