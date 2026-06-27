package com.dddblog.backend.blog.api;

import org.springframework.stereotype.Service;

import com.dddblog.backend.blog.application.PostDetail;
import com.dddblog.backend.blog.application.PostDetailQueryService;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.TagName;

@Service
public class PostDetailApiService {

	private final PostDetailQueryService postDetailQueryService;

	public PostDetailApiService(PostDetailQueryService postDetailQueryService) {
		this.postDetailQueryService = postDetailQueryService;
	}

	public PostDetailResponse getDetail(Long postId) {
		PostDetail postDetail = postDetailQueryService.getDetail(new PostId(postId));
		return new PostDetailResponse(
			postDetail.postId().value(),
			postDetail.authorId().value(),
			postDetail.title().value(),
			postDetail.contentType(),
			postDetail.content().value(),
			postDetail.summary().value(),
			postDetail.tags().stream()
				.map(TagName::value)
				.toList(),
			postDetail.status()
		);
	}
}
