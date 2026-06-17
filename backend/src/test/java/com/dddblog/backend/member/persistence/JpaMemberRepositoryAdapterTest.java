package com.dddblog.backend.member.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLIntegrityConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.MemberStatus;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;
import com.dddblog.backend.support.MysqlDataJpaTestSupport;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaMemberRepositoryAdapter.class)
class JpaMemberRepositoryAdapterTest extends MysqlDataJpaTestSupport {

	@Autowired
	private JpaMemberRepositoryAdapter memberRepository;

	@Autowired
	private SpringDataJpaMemberRepository springDataMemberRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 회원을_저장하면_ID를_반환한다() {
		Member member = createMember(10L, "user1", "닉네임1");

		MemberId memberId = memberRepository.save(member);

		assertThat(memberId).isEqualTo(new MemberId(10L));
	}

	@Test
	void 회원을_저장하면_members에_도메인_값이_저장된다() {
		Member member = createMember(20L, "user1", "닉네임1");

		memberRepository.save(member);

		entityManager.flush();
		entityManager.clear();

		JpaMemberEntity savedMember = springDataMemberRepository.findById(20L).orElseThrow();
		assertThat(savedMember.id()).isEqualTo(20L);
		assertThat(savedMember.name()).isEqualTo("홍길동");
		assertThat(savedMember.nickname()).isEqualTo("닉네임1");
		assertThat(savedMember.loginId()).isEqualTo("user1");
		assertThat(savedMember.passwordHash()).isEqualTo("hashed-password");
		assertThat(savedMember.role()).isEqualTo(MemberRole.MEMBER);
		assertThat(savedMember.status()).isEqualTo(MemberStatus.ACTIVE);
	}

	@Test
	void 저장된_로그인_ID가_있으면_존재한다고_판단한다() {
		memberRepository.save(createMember(1L, "user1", "닉네임1"));
		entityManager.flush();
		entityManager.clear();

		boolean exists = memberRepository.existsByLoginId(new LoginId("user1"));

		assertThat(exists).isTrue();
	}

	@Test
	void 저장된_닉네임이_있으면_존재한다고_판단한다() {
		memberRepository.save(createMember(1L, "user1", "닉네임1"));
		entityManager.flush();
		entityManager.clear();

		boolean exists = memberRepository.existsByNickname(new Nickname("닉네임1"));

		assertThat(exists).isTrue();
	}

	@Test
	void 로그인_ID는_중복_저장할_수_없다() {
		memberRepository.save(createMember(1L, "user1", "닉네임1"));
		entityManager.flush();
		entityManager.clear();

		assertThatThrownBy(() -> {
			memberRepository.save(createMember(2L, "user1", "닉네임2"));
			entityManager.flush();
		})
			.hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
	}

	@Test
	void 닉네임은_중복_저장할_수_없다() {
		memberRepository.save(createMember(1L, "user1", "닉네임1"));
		entityManager.flush();
		entityManager.clear();

		assertThatThrownBy(() -> {
			memberRepository.save(createMember(2L, "user2", "닉네임1"));
			entityManager.flush();
		})
			.hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
	}

	@Test
	void 같은_ID의_회원은_중복_저장할_수_없다() {
		memberRepository.save(createMember(30L, "user1", "닉네임1"));
		entityManager.flush();
		entityManager.clear();

		assertThatThrownBy(() -> {
			memberRepository.save(createMember(30L, "user2", "닉네임2"));
			entityManager.flush();
		})
			.hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
	}

	@Test
	void 로그인_ID로_회원을_조회할_수_있다() {
		memberRepository.save(createMember(40L, "user40", "닉네임40"));
		entityManager.flush();
		entityManager.clear();

		Member foundMember = memberRepository.findByLoginId(new LoginId("user40")).orElseThrow();

		assertThat(foundMember.id()).isEqualTo(new MemberId(40L));
		assertThat(foundMember.name()).isEqualTo(new MemberName("홍길동"));
		assertThat(foundMember.nickname()).isEqualTo(new Nickname("닉네임40"));
		assertThat(foundMember.loginId()).isEqualTo(new LoginId("user40"));
		assertThat(foundMember.passwordHash()).isEqualTo(new PasswordHash("hashed-password"));
		assertThat(foundMember.role()).isEqualTo(MemberRole.MEMBER);
		assertThat(foundMember.status()).isEqualTo(MemberStatus.ACTIVE);
	}

	@Test
	void 회원_ID로_회원을_조회할_수_있다() {
		memberRepository.save(createMember(50L, "user50", "닉네임50"));
		entityManager.flush();
		entityManager.clear();

		Member foundMember = memberRepository.findById(new MemberId(50L)).orElseThrow();

		assertThat(foundMember.id()).isEqualTo(new MemberId(50L));
		assertThat(foundMember.loginId()).isEqualTo(new LoginId("user50"));
	}

	@Test
	void 존재하지_않는_로그인_ID이면_빈_결과를_반환한다() {
		assertThat(memberRepository.findByLoginId(new LoginId("missing"))).isEmpty();
	}

	@Test
	void 존재하지_않는_회원_ID이면_빈_결과를_반환한다() {
		assertThat(memberRepository.findById(new MemberId(999L))).isEmpty();
	}

	@Test
	void 회원이_null이면_저장할_수_없다() {
		assertThatThrownBy(() -> memberRepository.save(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member must not be null.");
		assertThat(springDataMemberRepository.findAll()).isEmpty();
	}

	private Member createMember(Long id, String loginId, String nickname) {
		return Member.register(
			new MemberId(id),
			new MemberName("홍길동"),
			new Nickname(nickname),
			new LoginId(loginId),
			new PasswordHash("hashed-password")
		);
	}
}
