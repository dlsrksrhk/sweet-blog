package com.dddblog.backend.blog.domain;

import java.util.HashSet;
import java.util.List;

public final class Post {

	private static final int MAX_TAG_COUNT = 10;

	private final AuthorId authorId;
	private final PostTitle title;
	private final PostContent content;
	private final PostContentType contentType;
	private final PostSummary summary;
	private final List<TagName> tags;
	private final PostStatus status;

	public Post(
		AuthorId authorId,
		PostTitle title,
		PostContent content,
		PostContentType contentType,
		PostSummary summary,
		List<TagName> tags,
		PostStatus status
	) {
		if (authorId == null) {
			throw new IllegalArgumentException("Post author must not be null.");
		}
		if (title == null) {
			throw new IllegalArgumentException("Post title must not be null.");
		}
		if (content == null) {
			throw new IllegalArgumentException("Post content must not be null.");
		}
		if (contentType == null) {
			throw new IllegalArgumentException("Post content type must not be null.");
		}
		if (status == null) {
			throw new IllegalArgumentException("Post status must not be null.");
		}
		List<TagName> copiedTags = copyTags(tags);
		if (copiedTags.size() > MAX_TAG_COUNT) {
			throw new IllegalArgumentException("Post tags must be 10 or less.");
		}
		this.authorId = authorId;
		this.title = title;
		this.content = content;
		this.contentType = contentType;
		this.summary = summary == null ? new PostSummary(null) : summary;
		this.tags = copiedTags;
		this.status = status;
	}

	private List<TagName> copyTags(List<TagName> tags) {
		if (tags == null) {
			return List.of();
		}
		if (tags.stream().anyMatch(tag -> tag == null)) {
			throw new IllegalArgumentException("Post tag must not be null.");
		}
		validateDuplicatedTags(tags);
		return List.copyOf(tags);
	}

	private void validateDuplicatedTags(List<TagName> tags) {
		if (new HashSet<>(tags).size() != tags.size()) {
			throw new IllegalArgumentException("Post tags must not be duplicated.");
		}
	}

	public AuthorId authorId() {
		return authorId;
	}

	public PostTitle title() {
		return title;
	}

	public PostContent content() {
		return content;
	}

	public PostContentType contentType() {
		return contentType;
	}

	public PostSummary summary() {
		return summary;
	}

	public List<TagName> tags() {
		return tags;
	}

	public PostStatus status() {
		return status;
	}
}
