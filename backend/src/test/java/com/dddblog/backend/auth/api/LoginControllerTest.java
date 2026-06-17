package com.dddblog.backend.auth.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dddblog.backend.auth.application.AuthenticationFailedException;
import com.dddblog.backend.auth.application.LoginService;
import com.dddblog.backend.auth.security.JwtAuthenticationEntryPoint;
import com.dddblog.backend.common.api.GlobalExceptionHandler;
import com.dddblog.backend.config.SecurityConfig;

@WebMvcTest(LoginController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class LoginControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LoginService loginService;

	@Test
	void 로그인에_성공하면_200과_액세스_토큰을_반환한다() throws Exception {
		when(loginService.login(eq("user01"), eq("password123")))
			.thenReturn("access-token");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("access-token"));
	}

	@Test
	void 로그인에_실패하면_401과_오류_메시지를_반환한다() throws Exception {
		when(loginService.login(eq("user01"), eq("password123")))
			.thenThrow(new AuthenticationFailedException());

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Authentication failed."));
	}

	@Test
	void 인증이_필요한_요청은_401과_오류_메시지를_반환한다() throws Exception {
		mockMvc.perform(post("/api/protected"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Authentication failed."));
	}

	private String validJson() {
		return """
			{
			  "loginId": "user01",
			  "password": "password123"
			}
			""";
	}
}
