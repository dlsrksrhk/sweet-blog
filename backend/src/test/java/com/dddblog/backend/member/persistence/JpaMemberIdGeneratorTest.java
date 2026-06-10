package com.dddblog.backend.member.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.support.MysqlDataJpaTestSupport;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaMemberIdGenerator.class)
class JpaMemberIdGeneratorTest extends MysqlDataJpaTestSupport {

	@Autowired
	private JpaMemberIdGenerator memberIdGenerator;

	@Test
	void ID를_발급하면_양수_ID를_반환한다() {
		MemberId memberId = memberIdGenerator.nextId();

		assertThat(memberId.value()).isPositive();
	}

	@Test
	void ID를_연속으로_발급하면_서로_다른_ID를_반환한다() {
		MemberId firstMemberId = memberIdGenerator.nextId();
		MemberId secondMemberId = memberIdGenerator.nextId();

		assertThat(firstMemberId).isNotEqualTo(secondMemberId);
	}
}
