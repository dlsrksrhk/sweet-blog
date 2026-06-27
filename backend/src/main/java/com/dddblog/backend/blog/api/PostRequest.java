package com.dddblog.backend.blog.api;

import java.util.List;

import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostStatus;

public record PostRequest(
	String title,
	PostContentType contentType,
	String content,
	String summary,
	List<String> tags,
	PostStatus status
) {
}
