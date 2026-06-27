package com.dddblog.backend.blog.application;

import java.util.Optional;

import com.dddblog.backend.blog.domain.PostId;

public interface PostDetailQueryRepository {

	Optional<PostDetail> findPublishedById(PostId postId);
}
