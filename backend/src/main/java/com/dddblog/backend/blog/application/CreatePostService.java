package com.dddblog.backend.blog.application;

import java.util.List;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

public class CreatePostService {

	private final PostRepository postRepository;

	public CreatePostService(PostRepository postRepository) {
		this.postRepository = postRepository;
	}

	public PostId create(CreatePostCommand command) {
		if (command == null) {
			throw new IllegalArgumentException("Create post command must not be null.");
		}
		Post post = new Post(
			new AuthorId(command.authorId()),
			new PostTitle(command.title()),
			new PostContent(command.content()),
			command.contentType(),
			new PostSummary(command.summary()),
			toTagNames(command.tags()),
			command.status()
		);
		return postRepository.save(post);
	}

	private List<TagName> toTagNames(List<String> tags) {
		if (tags == null) {
			return List.of();
		}
		return tags.stream()
			.map(TagName::new)
			.toList();
	}
}
