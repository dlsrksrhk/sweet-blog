package com.dddblog.backend.blog.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.dddblog.backend.blog.domain.PostId;

public class FakePostDetailQueryRepository implements PostDetailQueryRepository {

	private final Map<PostId, PostDetail> postDetails = new HashMap<>();

	@Override
	public Optional<PostDetail> findPublishedById(PostId postId) {
		return Optional.ofNullable(postDetails.get(postId));
	}

	public void 저장한다(PostDetail postDetail) {
		postDetails.put(postDetail.postId(), postDetail);
	}
}
