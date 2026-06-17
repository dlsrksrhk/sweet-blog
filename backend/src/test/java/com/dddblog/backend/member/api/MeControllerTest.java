package com.dddblog.backend.member.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dddblog.backend.auth.application.AuthenticationFailedException;
import com.dddblog.backend.auth.security.AuthenticatedMember;
import com.dddblog.backend.auth.security.JwtAuthentication;
import com.dddblog.backend.auth.security.JwtAuthenticationEntryPoint;
import com.dddblog.backend.auth.security.JwtAuthenticationFilter;
import com.dddblog.backend.common.api.GlobalExceptionHandler;
import com.dddblog.backend.config.SecurityConfig;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WebMvcTest(MeController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class MeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MeService meService;

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@BeforeEach
	void setUp() throws Exception {
		doAnswer(invocation -> {
			ServletRequest request = invocation.getArgument(0);
			ServletResponse response = invocation.getArgument(1);
			FilterChain filterChain = invocation.getArgument(2);
			filterChain.doFilter(request, response);
			return null;
		}).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
	}

	@Test
	void 인증된_요청이면_내_정보를_반환한다() throws Exception {
		when(meService.getMe(eq(new MemberId(1L))))
			.thenReturn(new MeResponse(1L, "홍길동", "길동", "user01", "MEMBER"));

		mockMvc.perform(get("/api/members/me")
				.with(authentication(new JwtAuthentication(
					new AuthenticatedMember(new MemberId(1L), MemberRole.MEMBER)
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.memberId").value(1L))
			.andExpect(jsonPath("$.name").value("홍길동"))
			.andExpect(jsonPath("$.nickname").value("길동"))
			.andExpect(jsonPath("$.loginId").value("user01"))
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void 인증된_회원이_존재하지_않으면_401을_반환한다() throws Exception {
		when(meService.getMe(eq(new MemberId(1L))))
			.thenThrow(new AuthenticationFailedException());

		mockMvc.perform(get("/api/members/me")
				.with(authentication(new JwtAuthentication(
					new AuthenticatedMember(new MemberId(1L), MemberRole.MEMBER)
				))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Authentication failed."));
	}

	@Test
	void 토큰이_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/members/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Authentication failed."));
	}

	@Test
	void 인증_주체가_회원이_아니면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/members/me")
				.with(authentication(new TestingAuthenticationToken("invalid", "", "ROLE_MEMBER"))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Authentication failed."));
	}
}
