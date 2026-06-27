package com.dddblog.backend.blog.api;

import java.time.Instant;
import java.util.List;

import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostStatus;

public record PostDetailResponse(
	Long postId,
	Long authorId,
	String title,
	PostContentType contentType,
	String content,
	String summary,
	List<String> tags,
	PostStatus status,
	Instant createdAt,
	Instant updatedAt,
	Instant publishedAt
) {
}
