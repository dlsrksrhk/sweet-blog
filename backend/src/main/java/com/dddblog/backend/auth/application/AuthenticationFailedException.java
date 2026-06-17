package com.dddblog.backend.auth.application;

public class AuthenticationFailedException extends RuntimeException {

	public AuthenticationFailedException() {
		super("Authentication failed.");
	}
}
