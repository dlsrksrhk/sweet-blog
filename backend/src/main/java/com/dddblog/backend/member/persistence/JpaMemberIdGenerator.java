package com.dddblog.backend.member.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dddblog.backend.member.application.MemberIdGenerator;
import com.dddblog.backend.member.domain.MemberId;

import jakarta.annotation.PostConstruct;

@Repository
public class JpaMemberIdGenerator implements MemberIdGenerator {

	private static final String SEQUENCE_NAME = "member";

	private final SpringDataJpaMemberIdSequenceRepository memberIdSequenceRepository;

	public JpaMemberIdGenerator(SpringDataJpaMemberIdSequenceRepository memberIdSequenceRepository) {
		this.memberIdSequenceRepository = memberIdSequenceRepository;
	}

	@PostConstruct
	void initializeSequence() {
		if (memberIdSequenceRepository.existsById(SEQUENCE_NAME)) {
			return;
		}

		try {
			memberIdSequenceRepository.saveAndFlush(new JpaMemberIdSequenceEntity(SEQUENCE_NAME, 1L));
		}
		catch (DataIntegrityViolationException ignored) {
			// Another application instance initialized the sequence first.
		}
	}

	@Override
	@Transactional
	public MemberId nextId() {
		JpaMemberIdSequenceEntity sequence = memberIdSequenceRepository.findByNameWithLock(SEQUENCE_NAME)
			.orElseThrow(() -> new IllegalStateException("Member ID sequence is not initialized."));
		MemberId memberId = new MemberId(sequence.nextValue());

		sequence.increase();

		return memberId;
	}
}
