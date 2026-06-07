package com.dddblog.backend.member.domain;

public final class Member {

	private final MemberId id;
	private final MemberName name;
	private final Nickname nickname;
	private final LoginId loginId;
	private final PasswordHash passwordHash;
	private final MemberRole role;
	private final MemberStatus status;

	public Member(
		MemberId id,
		MemberName name,
		Nickname nickname,
		LoginId loginId,
		PasswordHash passwordHash,
		MemberRole role,
		MemberStatus status
	) {
		if (id == null) {
			throw new IllegalArgumentException("Member id must not be null.");
		}
		if (name == null) {
			throw new IllegalArgumentException("Member name must not be null.");
		}
		if (nickname == null) {
			throw new IllegalArgumentException("Member nickname must not be null.");
		}
		if (loginId == null) {
			throw new IllegalArgumentException("Member login id must not be null.");
		}
		if (passwordHash == null) {
			throw new IllegalArgumentException("Member password hash must not be null.");
		}
		if (role == null) {
			throw new IllegalArgumentException("Member role must not be null.");
		}
		if (status == null) {
			throw new IllegalArgumentException("Member status must not be null.");
		}
		this.id = id;
		this.name = name;
		this.nickname = nickname;
		this.loginId = loginId;
		this.passwordHash = passwordHash;
		this.role = role;
		this.status = status;
	}

	public static Member register(
		MemberId id,
		MemberName name,
		Nickname nickname,
		LoginId loginId,
		PasswordHash passwordHash
	) {
		return new Member(id, name, nickname, loginId, passwordHash, MemberRole.MEMBER, MemberStatus.ACTIVE);
	}

	public MemberId id() {
		return id;
	}

	public MemberName name() {
		return name;
	}

	public Nickname nickname() {
		return nickname;
	}

	public LoginId loginId() {
		return loginId;
	}

	public PasswordHash passwordHash() {
		return passwordHash;
	}

	public MemberRole role() {
		return role;
	}

	public MemberStatus status() {
		return status;
	}
}
