package com.dddblog.backend.blog.api;

import org.springframework.stereotype.Service;

import com.dddblog.backend.blog.application.PostListItem;
import com.dddblog.backend.blog.application.PostListPage;
import com.dddblog.backend.blog.application.PostListQuery;
import com.dddblog.backend.blog.application.PostListQueryService;
import com.dddblog.backend.blog.domain.TagName;

@Service
public class PostListApiService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;

	private final PostListQueryService postListQueryService;

	public PostListApiService(PostListQueryService postListQueryService) {
		this.postListQueryService = postListQueryService;
	}

	public PostListResponse getList(Integer page, Integer size) {
		PostListPage postListPage = postListQueryService.findPublished(new PostListQuery(
			page == null ? DEFAULT_PAGE : page,
			size == null ? DEFAULT_SIZE : size
		));
		return new PostListResponse(
			postListPage.items().stream()
				.map(this::toResponse)
				.toList(),
			postListPage.page(),
			postListPage.size(),
			postListPage.totalElements(),
			postListPage.totalPages(),
			postListPage.hasNext()
		);
	}

	private PostListItemResponse toResponse(PostListItem postListItem) {
		return new PostListItemResponse(
			postListItem.postId().value(),
			postListItem.authorId().value(),
			postListItem.title().value(),
			postListItem.summary().value(),
			postListItem.tags().stream()
				.map(TagName::value)
				.toList(),
			postListItem.status(),
			postListItem.createdAt(),
			postListItem.updatedAt(),
			postListItem.publishedAt()
		);
	}
}
