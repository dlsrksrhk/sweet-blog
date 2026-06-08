package com.dddblog.backend.member.application;

import java.util.ArrayList;
import java.util.List;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;

class FakeMemberRepository implements MemberRepository {

	private final List<Member> savedMembers = new ArrayList<>();
	private long nextId = 1L;

	@Override
	public boolean existsByLoginId(LoginId loginId) {
		return false;
	}

	@Override
	public boolean existsByNickname(Nickname nickname) {
		return false;
	}

	@Override
	public MemberId nextId() {
		return new MemberId(nextId++);
	}

	@Override
	public MemberId save(Member member) {
		savedMembers.add(member);
		return member.id();
	}

	List<Member> savedMembers() {
		return List.copyOf(savedMembers);
	}
}
