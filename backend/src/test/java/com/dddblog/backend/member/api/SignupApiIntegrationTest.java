package com.dddblog.backend.member.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dddblog.backend.support.MysqlDataJpaTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SignupApiIntegrationTest extends MysqlDataJpaTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 회원가입_API는_BCrypt로_해시한_비밀번호를_members에_저장한다() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "홍길동",
					  "nickname": "길동",
					  "loginId": "user01",
					  "password": "password123"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId").isNumber())
			.andReturn();

		Long responseMemberId = objectMapper.readTree(result.getResponse().getContentAsString())
			.get("memberId")
			.asLong();
		Long persistedMemberId = jdbcTemplate.queryForObject(
			"select id from members where login_id = ?",
			Long.class,
			"user01"
		);
		assertThat(responseMemberId).isEqualTo(persistedMemberId);

		String passwordHash = jdbcTemplate.queryForObject(
			"select password_hash from members where login_id = ?",
			String.class,
			"user01"
		);
		assertThat(passwordHash).isNotEqualTo("password123");
		assertThat(new BCryptPasswordEncoder().matches("password123", passwordHash)).isTrue();
	}
}
