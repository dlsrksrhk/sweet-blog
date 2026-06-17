package com.dddblog.backend.auth.application;

import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

public interface AccessTokenIssuer {

	String createAccessToken(MemberId memberId, MemberRole role);
}
