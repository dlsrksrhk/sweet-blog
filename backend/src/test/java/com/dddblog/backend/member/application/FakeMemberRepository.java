package com.dddblog.backend.member.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;

class FakeMemberRepository implements MemberRepository {

	private final List<Member> savedMembers = new ArrayList<>();
	private final Set<LoginId> existingLoginIds = new HashSet<>();
	private final Set<Nickname> existingNicknames = new HashSet<>();
	private long nextId = 1L;

	@Override
	public boolean existsByLoginId(LoginId loginId) {
		return existingLoginIds.contains(loginId);
	}

	@Override
	public boolean existsByNickname(Nickname nickname) {
		return existingNicknames.contains(nickname);
	}

	@Override
	public MemberId nextId() {
		return new MemberId(nextId++);
	}

	@Override
	public MemberId save(Member member) {
		savedMembers.add(member);
		existingLoginIds.add(member.loginId());
		existingNicknames.add(member.nickname());
		return member.id();
	}

	List<Member> savedMembers() {
		return List.copyOf(savedMembers);
	}

	FakeMemberRepository addExistingLoginId(LoginId loginId) {
		existingLoginIds.add(loginId);
		return this;
	}

	FakeMemberRepository addExistingNickname(Nickname nickname) {
		existingNicknames.add(nickname);
		return this;
	}
}
