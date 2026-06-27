package com.dddblog.backend.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dddblog.backend.blog.application.CreatePostService;
import com.dddblog.backend.blog.application.PostDetailQueryRepository;
import com.dddblog.backend.blog.application.PostDetailQueryService;
import com.dddblog.backend.blog.application.PostRepository;

@Configuration
public class BlogApplicationConfig {

	@Bean
	CreatePostService createPostService(PostRepository postRepository) {
		return new CreatePostService(postRepository);
	}

	@Bean
	PostDetailQueryService postDetailQueryService(PostDetailQueryRepository postDetailQueryRepository) {
		return new PostDetailQueryService(postDetailQueryRepository);
	}
}
