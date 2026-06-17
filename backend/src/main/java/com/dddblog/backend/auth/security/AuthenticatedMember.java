package com.dddblog.backend.auth.security;

import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

public record AuthenticatedMember(MemberId memberId, MemberRole role) {

	public AuthenticatedMember {
		if (memberId == null) {
			throw new IllegalArgumentException("Member id must not be null.");
		}
		if (role == null) {
			throw new IllegalArgumentException("Member role must not be null.");
		}
	}
}
