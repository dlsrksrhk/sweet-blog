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
		if (command == null) {
			throw new IllegalArgumentException("Register member command must not be null.");
		}

		MemberName name = new MemberName(command.name());
		Nickname nickname = new Nickname(command.nickname());
		LoginId loginId = new LoginId(command.loginId());
		PasswordHash passwordHash = new PasswordHash(command.passwordHash());
		if (memberRepository.existsByLoginId(loginId)) {
			throw new IllegalArgumentException("Login id already exists.");
		}
		if (memberRepository.existsByNickname(nickname)) {
			throw new IllegalArgumentException("Nickname already exists.");
		}

		MemberId memberId = memberRepository.nextId();
		Member member = Member.register(
			memberId,
			name,
			nickname,
			loginId,
			passwordHash
		);
		return memberRepository.save(member);
	}
}
