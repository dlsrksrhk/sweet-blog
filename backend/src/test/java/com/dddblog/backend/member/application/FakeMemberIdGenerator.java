package com.dddblog.backend.member.application;

import com.dddblog.backend.member.domain.MemberId;

class FakeMemberIdGenerator implements MemberIdGenerator {

	private long nextId = 1L;

	@Override
	public MemberId nextId() {
		return new MemberId(nextId++);
	}
}
