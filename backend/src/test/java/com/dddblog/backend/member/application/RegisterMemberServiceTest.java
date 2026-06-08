package com.dddblog.backend.member.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.member.domain.MemberId;

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

	private RegisterMemberCommand validCommand() {
		return new RegisterMemberCommand(
			"홍길동",
			"길동",
			"user01",
			"$2a$10$hashed-password"
		);
	}
}
