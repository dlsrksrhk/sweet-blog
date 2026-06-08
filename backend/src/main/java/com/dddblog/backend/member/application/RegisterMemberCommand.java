package com.dddblog.backend.member.application;

public record RegisterMemberCommand(
	String name,
	String nickname,
	String loginId,
	String passwordHash
) {
}
