package com.dddblog.backend.member.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dddblog.backend.member.application.MemberIdGenerator;
import com.dddblog.backend.member.domain.MemberId;

@Repository
public class JpaMemberIdGenerator implements MemberIdGenerator {

	private static final String SEQUENCE_NAME = "member";

	private final SpringDataJpaMemberIdSequenceRepository memberIdSequenceRepository;

	public JpaMemberIdGenerator(SpringDataJpaMemberIdSequenceRepository memberIdSequenceRepository) {
		this.memberIdSequenceRepository = memberIdSequenceRepository;
	}

	@Override
	@Transactional
	public MemberId nextId() {
		JpaMemberIdSequenceEntity sequence = memberIdSequenceRepository.findByNameWithLock(SEQUENCE_NAME)
			.orElseGet(() -> memberIdSequenceRepository.save(new JpaMemberIdSequenceEntity(SEQUENCE_NAME, 1L)));
		MemberId memberId = new MemberId(sequence.nextValue());

		sequence.increase();

		return memberId;
	}
}
