package com.dddblog.backend.member.application;

import com.dddblog.backend.member.domain.MemberId;

public interface MemberIdGenerator {

	MemberId nextId();
}
