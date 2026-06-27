package com.dddblog.backend.blog.application;

public class FakePostListQueryRepository implements PostListQueryRepository {

	private PostListQuery lastQuery;
	private PostListPage postListPage = new PostListPage(null, 0, 10, 0, 0, false);

	@Override
	public PostListPage findPublished(PostListQuery query) {
		lastQuery = query;
		return postListPage;
	}

	public void 결과를_준비한다(PostListPage postListPage) {
		this.postListPage = postListPage;
	}

	public PostListQuery 마지막_조회_조건() {
		return lastQuery;
	}
}
