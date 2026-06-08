package com.dddblog.backend.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.MemberRole;
import com.dddblog.backend.member.domain.MemberStatus;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

class RegisterMemberServiceTest {

	@Test
	void 유효한_요청이면_회원을_저장하고_ID를_반환한다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		MemberId memberId = service.register(command);

		assertThat(memberId).isEqualTo(new MemberId(1L));
		assertThat(memberRepository.savedMembers()).hasSize(1);
	}

	@Test
	void 저장된_회원은_요청_값을_도메인_값으로_가진다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		service.register(command);

		Member savedMember = memberRepository.savedMembers().get(0);
		assertThat(savedMember.id()).isEqualTo(new MemberId(1L));
		assertThat(savedMember.name()).isEqualTo(new MemberName("홍길동"));
		assertThat(savedMember.nickname()).isEqualTo(new Nickname("길동"));
		assertThat(savedMember.loginId()).isEqualTo(new LoginId("user01"));
		assertThat(savedMember.passwordHash()).isEqualTo(new PasswordHash("$2a$10$hashed-password"));
	}

	@Test
	void 신규_회원은_MEMBER_권한과_ACTIVE_상태를_가진다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		service.register(command);

		Member savedMember = memberRepository.savedMembers().get(0);
		assertThat(savedMember.role()).isEqualTo(MemberRole.MEMBER);
		assertThat(savedMember.status()).isEqualTo(MemberStatus.ACTIVE);
	}

	@Test
	void command가_null이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);

		assertThatThrownBy(() -> service.register(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Register member command must not be null.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 잘못된_로그인_ID이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = new RegisterMemberCommand(
			"홍길동",
			"길동",
			"abc",
			"$2a$10$hashed-password"
		);

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id must be 4 characters or more.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 잘못된_닉네임이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = new RegisterMemberCommand(
			"홍길동",
			"길",
			"user01",
			"$2a$10$hashed-password"
		);

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname must be 2 characters or more.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 잘못된_비밀번호_해시이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = new RegisterMemberCommand(
			"홍길동",
			"길동",
			"user01",
			" "
		);

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password hash must not be blank.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	private RegisterMemberCommand validCommand() {
		return new RegisterMemberCommand(
			"홍길동",
			"길동",
			"user01",
			"$2a$10$hashed-password"
		);
	}
}
