package com.dddblog.backend.member.persistence;

import com.dddblog.backend.member.domain.MemberRole;
import com.dddblog.backend.member.domain.MemberStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "members",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = "nickname"),
		@UniqueConstraint(columnNames = "login_id")
	}
)
class JpaMemberEntity {

	@Id
	private Long id;

	@Column(nullable = false, length = 30)
	private String name;

	@Column(nullable = false, length = 20)
	private String nickname;

	@Column(name = "login_id", nullable = false, length = 30)
	private String loginId;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MemberRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MemberStatus status;

	protected JpaMemberEntity() {
	}

	JpaMemberEntity(
		Long id,
		String name,
		String nickname,
		String loginId,
		String passwordHash,
		MemberRole role,
		MemberStatus status
	) {
		this.id = id;
		this.name = name;
		this.nickname = nickname;
		this.loginId = loginId;
		this.passwordHash = passwordHash;
		this.role = role;
		this.status = status;
	}

	Long id() {
		return id;
	}

	String name() {
		return name;
	}

	String nickname() {
		return nickname;
	}

	String loginId() {
		return loginId;
	}

	String passwordHash() {
		return passwordHash;
	}

	MemberRole role() {
		return role;
	}

	MemberStatus status() {
		return status;
	}
}
