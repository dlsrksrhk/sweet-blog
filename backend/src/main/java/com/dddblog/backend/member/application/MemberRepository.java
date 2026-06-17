package com.dddblog.backend.member.application;

import java.util.Optional;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;

public interface MemberRepository {

	boolean existsByLoginId(LoginId loginId);

	boolean existsByNickname(Nickname nickname);

	Optional<Member> findByLoginId(LoginId loginId);

	Optional<Member> findById(MemberId memberId);

	MemberId save(Member member);
}
