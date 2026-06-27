package com.dddblog.backend.blog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.dddblog.backend.support.MysqlDataJpaTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostApiIntegrationTest extends MysqlDataJpaTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 회원가입_후_로그인하면_발급된_토큰으로_글을_작성할_수_있다() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "홍길동",
					  "nickname": "길동",
					  "loginId": "user01",
					  "password": "password123"
					}
					"""))
			.andExpect(status().isCreated());

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "user01",
					  "password": "password123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isString())
			.andReturn();

		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
			.get("accessToken")
			.asText();

		MvcResult createPostResult = mockMvc.perform(post("/api/posts")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "title": "DDD 시작하기",
					  "contentType": "MARKDOWN",
					  "content": "# DDD\\n\\n본문",
					  "summary": "DDD 소개",
					  "tags": ["DDD", "TDD"],
					  "status": "PUBLISHED"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.postId").isNumber())
			.andReturn();

		Long postId = objectMapper.readTree(createPostResult.getResponse().getContentAsString())
			.get("postId")
			.asLong();
		Long memberId = jdbcTemplate.queryForObject(
			"select id from members where login_id = ?",
			Long.class,
			"user01"
		);

		Long authorId = jdbcTemplate.queryForObject(
			"select author_id from posts where id = ?",
			Long.class,
			postId
		);
		String contentType = jdbcTemplate.queryForObject(
			"select content_type from posts where id = ?",
			String.class,
			postId
		);
		String contentMarkdown = jdbcTemplate.queryForObject(
			"select content_markdown from posts where id = ?",
			String.class,
			postId
		);
		String status = jdbcTemplate.queryForObject(
			"select status from posts where id = ?",
			String.class,
			postId
		);
		List<String> tagNames = jdbcTemplate.queryForList(
			"""
			select t.name
			from tags t
			join post_tags pt on pt.tag_id = t.id
			where pt.post_id = ?
			order by t.name
			""",
			String.class,
			postId
		);

		assertThat(authorId).isEqualTo(memberId);
		assertThat(contentType).isEqualTo("MARKDOWN");
		assertThat(contentMarkdown).isEqualTo("# DDD\n\n본문");
		assertThat(status).isEqualTo("PUBLISHED");
		assertThat(tagNames).containsExactly("ddd", "tdd");
	}
}
