package com.dddblog.backend.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dddblog.backend.member.application.MemberIdGenerator;
import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.application.RegisterMemberService;

@Configuration
public class MemberApplicationConfig {

	@Bean
	RegisterMemberService registerMemberService(
		MemberRepository memberRepository,
		MemberIdGenerator memberIdGenerator
	) {
		return new RegisterMemberService(memberRepository, memberIdGenerator);
	}
}
