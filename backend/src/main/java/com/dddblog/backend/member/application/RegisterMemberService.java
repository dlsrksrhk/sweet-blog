package com.dddblog.backend.member.application;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

public class RegisterMemberService {

	private final MemberRepository memberRepository;

	public RegisterMemberService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public MemberId register(RegisterMemberCommand command) {
		MemberId memberId = memberRepository.nextId();
		Member member = Member.register(
			memberId,
			new MemberName(command.name()),
			new Nickname(command.nickname()),
			new LoginId(command.loginId()),
			new PasswordHash(command.passwordHash())
		);
		return memberRepository.save(member);
	}
}
