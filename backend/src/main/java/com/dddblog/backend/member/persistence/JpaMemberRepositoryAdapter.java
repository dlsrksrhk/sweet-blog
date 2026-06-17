package com.dddblog.backend.member.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

import jakarta.persistence.EntityManager;

@Repository
public class JpaMemberRepositoryAdapter implements MemberRepository {

	private final SpringDataJpaMemberRepository memberRepository;
	private final EntityManager entityManager;

	public JpaMemberRepositoryAdapter(SpringDataJpaMemberRepository memberRepository, EntityManager entityManager) {
		this.memberRepository = memberRepository;
		this.entityManager = entityManager;
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
	public Optional<Member> findByLoginId(LoginId loginId) {
		return memberRepository.findByLoginId(loginId.value())
			.map(this::toDomain);
	}

	@Override
	public Optional<Member> findById(MemberId memberId) {
		return memberRepository.findById(memberId.value())
			.map(this::toDomain);
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
		entityManager.persist(entity);
		return new MemberId(entity.id());
	}

	private Member toDomain(JpaMemberEntity entity) {
		return new Member(
			new MemberId(entity.id()),
			new MemberName(entity.name()),
			new Nickname(entity.nickname()),
			new LoginId(entity.loginId()),
			new PasswordHash(entity.passwordHash()),
			entity.role(),
			entity.status()
		);
	}
}
