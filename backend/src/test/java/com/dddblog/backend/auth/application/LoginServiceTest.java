package com.dddblog.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.MemberRole;
import com.dddblog.backend.member.domain.MemberStatus;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

class LoginServiceTest {

	private final FakeMemberRepository memberRepository = new FakeMemberRepository();
	private final FakePasswordEncoder passwordEncoder = new FakePasswordEncoder();
	private final FakeAccessTokenIssuer tokenIssuer = new FakeAccessTokenIssuer();
	private final LoginService loginService = new LoginService(memberRepository, passwordEncoder, tokenIssuer);

	@Test
	void 올바른_로그인_ID와_비밀번호이면_액세스_토큰을_발급한다() {
		memberRepository.save(activeMember());

		String accessToken = loginService.login("user01", "password123");

		assertThat(accessToken).isEqualTo("token-1-MEMBER");
	}

	@Test
	void 존재하지_않는_로그인_ID이면_인증에_실패한다() {
		assertThatThrownBy(() -> loginService.login("missing", "password123"))
			.isInstanceOf(AuthenticationFailedException.class)
			.hasMessage("Authentication failed.");
	}

	@Test
	void 잘못된_로그인_ID이면_인증에_실패한다() {
		assertThatThrownBy(() -> loginService.login("abc", "password123"))
			.isInstanceOf(AuthenticationFailedException.class)
			.hasMessage("Authentication failed.");
	}

	@Test
	void 비밀번호가_일치하지_않으면_인증에_실패한다() {
		memberRepository.save(activeMember());

		assertThatThrownBy(() -> loginService.login("user01", "wrong-password"))
			.isInstanceOf(AuthenticationFailedException.class)
			.hasMessage("Authentication failed.");
	}

	@Test
	void 비밀번호가_없으면_인증에_실패한다() {
		memberRepository.save(activeMember());

		assertThatThrownBy(() -> loginService.login("user01", null))
			.isInstanceOf(AuthenticationFailedException.class)
			.hasMessage("Authentication failed.");
	}

	@Test
	void 비활성_회원이면_인증에_실패한다() {
		memberRepository.save(inactiveMember());

		assertThatThrownBy(() -> loginService.login("user01", "password123"))
			.isInstanceOf(AuthenticationFailedException.class)
			.hasMessage("Authentication failed.");
	}

	private Member activeMember() {
		return new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("encoded-password123"),
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		);
	}

	private Member inactiveMember() {
		return new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("encoded-password123"),
			MemberRole.MEMBER,
			MemberStatus.INACTIVE
		);
	}

	private static class FakeMemberRepository implements MemberRepository {

		private Member member;

		@Override
		public boolean existsByLoginId(LoginId loginId) {
			return false;
		}

		@Override
		public boolean existsByNickname(Nickname nickname) {
			return false;
		}

		@Override
		public Optional<Member> findByLoginId(LoginId loginId) {
			if (member != null && member.loginId().equals(loginId)) {
				return Optional.of(member);
			}
			return Optional.empty();
		}

		@Override
		public Optional<Member> findById(MemberId memberId) {
			if (member != null && member.id().equals(memberId)) {
				return Optional.of(member);
			}
			return Optional.empty();
		}

		@Override
		public MemberId save(Member member) {
			this.member = member;
			return member.id();
		}
	}

	private static class FakePasswordEncoder implements PasswordEncoder {

		@Override
		public String encode(CharSequence rawPassword) {
			return "encoded-" + rawPassword;
		}

		@Override
		public boolean matches(CharSequence rawPassword, String encodedPassword) {
			if (rawPassword == null) {
				throw new IllegalArgumentException("Raw password must not be null.");
			}
			return encode(rawPassword).equals(encodedPassword);
		}
	}

	private static class FakeAccessTokenIssuer implements AccessTokenIssuer {

		@Override
		public String createAccessToken(MemberId memberId, MemberRole role) {
			return "token-" + memberId.value() + "-" + role.name();
		}
	}
}
