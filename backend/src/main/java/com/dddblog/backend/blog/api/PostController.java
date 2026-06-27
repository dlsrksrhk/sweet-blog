package com.dddblog.backend.blog.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dddblog.backend.auth.application.AuthenticationFailedException;
import com.dddblog.backend.auth.security.AuthenticatedMember;

@RestController
@RequestMapping("/api/posts")
public class PostController {

	private final PostApiService postApiService;

	public PostController(PostApiService postApiService) {
		this.postApiService = postApiService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PostResponse create(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody PostRequest request
	) {
		if (authenticatedMember == null) {
			throw new AuthenticationFailedException();
		}
		return postApiService.create(authenticatedMember.memberId(), request);
	}
}
