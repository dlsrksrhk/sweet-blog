package com.dddblog.backend.blog.application;

public interface PostListQueryRepository {

	PostListPage findPublished(PostListQuery query);
}
