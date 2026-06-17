package com.dddblog.backend.member.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dddblog.backend.auth.security.JwtAuthenticationEntryPoint;
import com.dddblog.backend.auth.security.JwtAuthenticationFilter;
import com.dddblog.backend.common.api.GlobalExceptionHandler;
import com.dddblog.backend.config.SecurityConfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WebMvcTest(SignupController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class SignupControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SignupService signupService;

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
	void 회원가입에_성공하면_201과_회원_ID를_반환한다() throws Exception {
		when(signupService.signup(any(SignupRequest.class)))
			.thenReturn(new SignupResponse(1L));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId").value(1L));
	}

	@Test
	void 회원가입_요청이_실패하면_400과_오류_메시지를_반환한다() throws Exception {
		when(signupService.signup(any(SignupRequest.class)))
			.thenThrow(new IllegalArgumentException("Login id already exists."));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Login id already exists."));
	}

	@Test
	void 인증_없이_회원가입을_요청할_수_있다() throws Exception {
		when(signupService.signup(any(SignupRequest.class)))
			.thenReturn(new SignupResponse(1L));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isCreated());
	}

	private String validJson() {
		return """
			{
			  "name": "홍길동",
			  "nickname": "길동",
			  "loginId": "user01",
			  "password": "password123"
			}
			""";
	}
}
