package com.dddblog.backend.member.api;

public record SignupRequest(
	String name,
	String nickname,
	String loginId,
	String password
) {
}
