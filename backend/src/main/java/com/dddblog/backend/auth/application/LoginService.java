package com.dddblog.backend.auth.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberStatus;

@Service
public class LoginService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final AccessTokenIssuer accessTokenIssuer;

	public LoginService(
		MemberRepository memberRepository,
		PasswordEncoder passwordEncoder,
		AccessTokenIssuer accessTokenIssuer
	) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.accessTokenIssuer = accessTokenIssuer;
	}

	public String login(String loginId, String password) {
		LoginId memberLoginId = toLoginId(loginId);
		Member member = memberRepository.findByLoginId(memberLoginId)
			.orElseThrow(AuthenticationFailedException::new);
		if (password == null) {
			throw new AuthenticationFailedException();
		}
		if (!passwordEncoder.matches(password, member.passwordHash().value())) {
			throw new AuthenticationFailedException();
		}
		if (member.status() != MemberStatus.ACTIVE) {
			throw new AuthenticationFailedException();
		}
		return accessTokenIssuer.createAccessToken(member.id(), member.role());
	}

	private LoginId toLoginId(String loginId) {
		try {
			return new LoginId(loginId);
		} catch (IllegalArgumentException exception) {
			throw new AuthenticationFailedException();
		}
	}
}
