package com.dddblog.backend.member.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;

@Repository
public class JpaMemberRepositoryAdapter implements MemberRepository {

	private final SpringDataJpaMemberRepository memberRepository;

	public JpaMemberRepositoryAdapter(SpringDataJpaMemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Override
	public boolean existsByLoginId(LoginId loginId) {
		return memberRepository.existsByLoginId(loginId.value());
	}

	@Override
	public boolean existsByNickname(Nickname nickname) {
		return memberRepository.existsByNickname(nickname.value());
	}

	@Override
	@Transactional
	public MemberId save(Member member) {
		if (member == null) {
			throw new IllegalArgumentException("Member must not be null.");
		}
		JpaMemberEntity entity = new JpaMemberEntity(
			member.id().value(),
			member.name().value(),
			member.nickname().value(),
			member.loginId().value(),
			member.passwordHash().value(),
			member.role(),
			member.status()
		);
		JpaMemberEntity savedEntity = memberRepository.save(entity);
		return new MemberId(savedEntity.id());
	}
}
