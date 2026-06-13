package com.dddblog.backend.member.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dddblog.backend.member.application.MemberIdGenerator;
import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.application.RegisterMemberService;
import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

class SignupServiceTest {

	@Test
	void 회원가입을_요청하면_비밀번호를_해시해서_회원가입_서비스에_전달한다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		FakePasswordEncoder passwordEncoder = new FakePasswordEncoder();
		RegisterMemberService registerMemberService = new RegisterMemberService(memberRepository, memberIdGenerator);
		SignupService signupService = new SignupService(registerMemberService, passwordEncoder);

		signupService.signup(validRequest());

		Member savedMember = memberRepository.savedMembers().get(0);
		assertThat(passwordEncoder.encodedPasswords()).containsExactly("password123");
		assertThat(savedMember.passwordHash()).isEqualTo(new PasswordHash("$2a$10$encoded-password"));
	}

	@Test
	void 회원가입에_성공하면_회원_ID를_반환한다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		RegisterMemberService registerMemberService = new RegisterMemberService(memberRepository, memberIdGenerator);
		SignupService signupService = new SignupService(registerMemberService, new FakePasswordEncoder());

		SignupResponse response = signupService.signup(validRequest());

		assertThat(response.memberId()).isEqualTo(1L);
	}

	@Test
	void 비밀번호가_8자_미만이면_회원가입_서비스를_호출하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		FakePasswordEncoder passwordEncoder = new FakePasswordEncoder();
		RegisterMemberService registerMemberService = new RegisterMemberService(memberRepository, memberIdGenerator);
		SignupService signupService = new SignupService(registerMemberService, passwordEncoder);
		SignupRequest request = new SignupRequest("홍길동", "길동", "user01", "1234567");

		assertThatThrownBy(() -> signupService.signup(request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password must be at least 8 characters.");
		assertThat(passwordEncoder.encodedPasswords()).isEmpty();
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	private SignupRequest validRequest() {
		return new SignupRequest("홍길동", "길동", "user01", "password123");
	}

	private static class FakePasswordEncoder implements PasswordEncoder {

		private final List<String> encodedPasswords = new ArrayList<>();

		@Override
		public String encode(CharSequence rawPassword) {
			encodedPasswords.add(rawPassword.toString());
			return "$2a$10$encoded-password";
		}

		@Override
		public boolean matches(CharSequence rawPassword, String encodedPassword) {
			return "$2a$10$encoded-password".equals(encodedPassword);
		}

		List<String> encodedPasswords() {
			return List.copyOf(encodedPasswords);
		}
	}

	private static class FakeMemberRepository implements MemberRepository {

		private final List<Member> savedMembers = new ArrayList<>();
		private final Set<LoginId> existingLoginIds = new HashSet<>();
		private final Set<Nickname> existingNicknames = new HashSet<>();

		@Override
		public boolean existsByLoginId(LoginId loginId) {
			return existingLoginIds.contains(loginId);
		}

		@Override
		public boolean existsByNickname(Nickname nickname) {
			return existingNicknames.contains(nickname);
		}

		@Override
		public MemberId save(Member member) {
			savedMembers.add(member);
			existingLoginIds.add(member.loginId());
			existingNicknames.add(member.nickname());
			return member.id();
		}

		List<Member> savedMembers() {
			return List.copyOf(savedMembers);
		}
	}

	private static class FakeMemberIdGenerator implements MemberIdGenerator {

		private long nextId = 1L;

		@Override
		public MemberId nextId() {
			return new MemberId(nextId++);
		}
	}
}
