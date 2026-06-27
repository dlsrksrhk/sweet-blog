package com.dddblog.backend.blog.api;

import org.springframework.stereotype.Service;

import com.dddblog.backend.blog.application.CreatePostCommand;
import com.dddblog.backend.blog.application.CreatePostService;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.member.domain.MemberId;

@Service
public class PostApiService {

	private final CreatePostService createPostService;

	public PostApiService(CreatePostService createPostService) {
		this.createPostService = createPostService;
	}

	public PostResponse create(MemberId memberId, PostRequest request) {
		validateMarkdownContentType(request.contentType());
		CreatePostCommand command = new CreatePostCommand(
			memberId.value(),
			request.title(),
			request.contentType(),
			request.content(),
			request.summary(),
			request.tags(),
			request.status()
		);
		PostId postId = createPostService.create(command);
		return new PostResponse(postId.value());
	}

	private void validateMarkdownContentType(PostContentType contentType) {
		if (contentType != PostContentType.MARKDOWN) {
			throw new IllegalArgumentException("Post content type must be MARKDOWN.");
		}
	}
}
