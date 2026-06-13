package com.dddblog.backend.member.api;

import static org.mockito.ArgumentMatchers.any;
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

import com.dddblog.backend.common.api.GlobalExceptionHandler;
import com.dddblog.backend.config.SecurityConfig;

@WebMvcTest(SignupController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class SignupControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SignupService signupService;

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
