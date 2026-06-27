package com.dddblog.backend.blog.application;

import java.util.List;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

public record PostDetail(
	PostId postId,
	AuthorId authorId,
	PostTitle title,
	PostContentType contentType,
	PostContent content,
	PostSummary summary,
	List<TagName> tags,
	PostStatus status
) {

	public PostDetail {
		if (postId == null) {
			throw new IllegalArgumentException("Post id must not be null.");
		}
		if (authorId == null) {
			throw new IllegalArgumentException("Post author must not be null.");
		}
		if (title == null) {
			throw new IllegalArgumentException("Post title must not be null.");
		}
		if (contentType == null) {
			throw new IllegalArgumentException("Post content type must not be null.");
		}
		if (content == null) {
			throw new IllegalArgumentException("Post content must not be null.");
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
		tags = List.copyOf(tags);
	}
}
