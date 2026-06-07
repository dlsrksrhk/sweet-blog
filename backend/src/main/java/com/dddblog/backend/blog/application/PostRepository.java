package com.dddblog.backend.blog.application;

import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostId;

public interface PostRepository {

	PostId save(Post post);
}
