package com.dddblog.backend.blog.application;

import java.util.ArrayList;
import java.util.List;

import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostId;

public class FakePostRepository implements PostRepository {

	private final List<Post> savedPosts = new ArrayList<>();

	@Override
	public PostId save(Post post) {
		savedPosts.add(post);
		return new PostId((long) savedPosts.size());
	}

	public List<Post> savedPosts() {
		return List.copyOf(savedPosts);
	}
}
