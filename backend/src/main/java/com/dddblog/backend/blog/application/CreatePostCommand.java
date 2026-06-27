package com.dddblog.backend.blog.application;

import java.util.List;

import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostStatus;

public record CreatePostCommand(
	Long authorId,
	String title,
	PostContentType contentType,
	String content,
	String summary,
	List<String> tags,
	PostStatus status
) {
}
