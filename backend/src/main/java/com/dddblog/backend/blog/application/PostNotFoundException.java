package com.dddblog.backend.blog.application;

public class PostNotFoundException extends RuntimeException {

	public PostNotFoundException() {
		super("Post not found.");
	}
}
