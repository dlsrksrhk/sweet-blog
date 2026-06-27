package com.dddblog.backend.blog.application;

import java.time.Instant;
import java.util.List;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

public record PostListItem(
	PostId postId,
	AuthorId authorId,
	PostTitle title,
	PostSummary summary,
	List<TagName> tags,
	PostStatus status,
	Instant createdAt,
	Instant updatedAt,
	Instant publishedAt
) {

	public PostListItem {
		if (postId == null) {
			throw new IllegalArgumentException("Post id must not be null.");
		}
		if (authorId == null) {
			throw new IllegalArgumentException("Post author must not be null.");
		}
		if (title == null) {
			throw new IllegalArgumentException("Post title must not be null.");
		}
		if (summary == null) {
			summary = new PostSummary(null);
		}
		if (tags == null) {
			tags = List.of();
		}
		if (tags.stream().anyMatch(tag -> tag == null)) {
			throw new IllegalArgumentException("Post tag must not be null.");
		}
		if (status == null) {
			throw new IllegalArgumentException("Post status must not be null.");
		}
		if (createdAt == null) {
			throw new IllegalArgumentException("Post created at must not be null.");
		}
		if (updatedAt == null) {
			throw new IllegalArgumentException("Post updated at must not be null.");
		}
		if (publishedAt == null) {
			throw new IllegalArgumentException("Post published at must not be null.");
		}
		tags = List.copyOf(tags);
	}
}
