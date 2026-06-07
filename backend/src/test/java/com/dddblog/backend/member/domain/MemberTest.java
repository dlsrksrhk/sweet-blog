package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MemberTest {

	@Test
	void 신규_회원을_등록한다() {
		MemberId id = new MemberId(1L);
		MemberName name = new MemberName("홍길동");
		Nickname nickname = new Nickname("길동");
		LoginId loginId = new LoginId("user01");
		PasswordHash passwordHash = new PasswordHash("$2a$10$hashed-password");

		Member member = Member.register(id, name, nickname, loginId, passwordHash);

		assertThat(member.id()).isEqualTo(id);
		assertThat(member.name()).isEqualTo(name);
		assertThat(member.nickname()).isEqualTo(nickname);
		assertThat(member.loginId()).isEqualTo(loginId);
		assertThat(member.passwordHash()).isEqualTo(passwordHash);
	}

	@Test
	void 신규_회원은_MEMBER_권한을_가진다() {
		Member member = registerMember();

		assertThat(member.role()).isEqualTo(MemberRole.MEMBER);
	}

	@Test
	void 신규_회원은_ACTIVE_상태를_가진다() {
		Member member = registerMember();

		assertThat(member.status()).isEqualTo(MemberStatus.ACTIVE);
	}

	@Test
	void ID가_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			null,
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("$2a$10$hashed-password"),
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member id must not be null.");
	}

	@Test
	void 이름이_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			new MemberId(1L),
			null,
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("$2a$10$hashed-password"),
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member name must not be null.");
	}

	@Test
	void 닉네임이_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			null,
			new LoginId("user01"),
			new PasswordHash("$2a$10$hashed-password"),
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member nickname must not be null.");
	}

	@Test
	void 로그인_ID가_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			null,
			new PasswordHash("$2a$10$hashed-password"),
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member login id must not be null.");
	}

	@Test
	void 비밀번호_해시가_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			null,
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member password hash must not be null.");
	}

	@Test
	void 권한이_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("$2a$10$hashed-password"),
			null,
			MemberStatus.ACTIVE
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member role must not be null.");
	}

	@Test
	void 상태가_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("$2a$10$hashed-password"),
			MemberRole.MEMBER,
			null
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member status must not be null.");
	}

	private Member registerMember() {
		return Member.register(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("$2a$10$hashed-password")
		);
	}
}
