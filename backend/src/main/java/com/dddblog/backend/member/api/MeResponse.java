package com.dddblog.backend.member.api;

public record MeResponse(
	Long memberId,
	String name,
	String nickname,
	String loginId,
	String role
) {
}
