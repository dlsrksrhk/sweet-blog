package com.dddblog.backend.member.api;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dddblog.backend.member.application.RegisterMemberCommand;
import com.dddblog.backend.member.application.RegisterMemberService;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.RawPassword;

@Service
public class SignupService {

	private final RegisterMemberService registerMemberService;
	private final PasswordEncoder passwordEncoder;

	public SignupService(RegisterMemberService registerMemberService, PasswordEncoder passwordEncoder) {
		this.registerMemberService = registerMemberService;
		this.passwordEncoder = passwordEncoder;
	}

	public SignupResponse signup(SignupRequest request) {
		RawPassword rawPassword = new RawPassword(request.password());
		String passwordHash = passwordEncoder.encode(rawPassword.value());
		RegisterMemberCommand command = new RegisterMemberCommand(
			request.name(),
			request.nickname(),
			request.loginId(),
			passwordHash
		);
		MemberId memberId = registerMemberService.register(command);

		return new SignupResponse(memberId.value());
	}
}
