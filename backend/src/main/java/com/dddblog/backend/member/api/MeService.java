package com.dddblog.backend.member.api;

import org.springframework.stereotype.Service;

import com.dddblog.backend.auth.application.AuthenticationFailedException;
import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;

@Service
public class MeService {

	private final MemberRepository memberRepository;

	public MeService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public MeResponse getMe(MemberId memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(AuthenticationFailedException::new);
		return new MeResponse(
			member.id().value(),
			member.name().value(),
			member.nickname().value(),
			member.loginId().value(),
			member.role().name()
		);
	}
}
