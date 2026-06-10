package com.dddblog.backend.member.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.support.MysqlDataJpaTestSupport;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaMemberIdGenerator.class)
class JpaMemberIdGeneratorTest extends MysqlDataJpaTestSupport {

	@Autowired
	private JpaMemberIdGenerator memberIdGenerator;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private SpringDataJpaMemberIdSequenceRepository memberIdSequenceRepository;

	@Test
	void 시퀀스_row를_미리_준비한다() {
		assertThat(memberIdSequenceRepository.findById("member"))
			.hasValueSatisfying(sequence -> assertThat(sequence.nextValue()).isEqualTo(1L));
	}

	@Test
	void ID를_발급하면_양수_ID를_반환한다() {
		MemberId memberId = memberIdGenerator.nextId();

		assertThat(memberId).isEqualTo(new MemberId(1L));
	}

	@Test
	void ID를_연속으로_발급하면_서로_다른_ID를_반환한다() {
		MemberId firstMemberId = memberIdGenerator.nextId();
		MemberId secondMemberId = memberIdGenerator.nextId();

		assertThat(firstMemberId).isEqualTo(new MemberId(1L));
		assertThat(secondMemberId).isEqualTo(new MemberId(2L));
	}

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void 트랜잭션이_달라도_증가한_ID를_반환한다() {
		MemberId firstMemberId = nextIdInNewTransaction();
		MemberId secondMemberId = nextIdInNewTransaction();

		assertThat(firstMemberId).isEqualTo(new MemberId(1L));
		assertThat(secondMemberId).isEqualTo(new MemberId(2L));
	}

	private MemberId nextIdInNewTransaction() {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return transactionTemplate.execute(status -> memberIdGenerator.nextId());
	}
}
