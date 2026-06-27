package com.dddblog.backend.blog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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

	@BeforeEach
	void 테스트_데이터를_정리한다() {
		jdbcTemplate.update("delete from post_tags");
		jdbcTemplate.update("delete from posts");
		jdbcTemplate.update("delete from tags");
		jdbcTemplate.update("delete from members");
		jdbcTemplate.update("update member_id_sequences set next_value = 1 where name = 'member'");
	}

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
		Object createdAt = jdbcTemplate.queryForObject(
			"select created_at from posts where id = ?",
			Object.class,
			postId
		);
		Object updatedAt = jdbcTemplate.queryForObject(
			"select updated_at from posts where id = ?",
			Object.class,
			postId
		);
		Object publishedAt = jdbcTemplate.queryForObject(
			"select published_at from posts where id = ?",
			Object.class,
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
		assertThat(createdAt).isNotNull();
		assertThat(updatedAt).isNotNull();
		assertThat(publishedAt).isNotNull();
		assertThat(tagNames).containsExactly("ddd", "tdd");
	}

	@Test
	void 회원가입_후_작성한_공개_글을_토큰_없이_상세_조회할_수_있다() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "김철수",
					  "nickname": "철수",
					  "loginId": "user02",
					  "password": "password123"
					}
					"""))
			.andExpect(status().isCreated());

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "user02",
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
					  "title": "공개 글",
					  "contentType": "MARKDOWN",
					  "content": "# 공개 글\\n\\n본문",
					  "summary": "공개 글 소개",
					  "tags": ["TDD", "DDD"],
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
			"user02"
		);

		mockMvc.perform(get("/api/posts/{postId}", postId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.postId").value(postId))
			.andExpect(jsonPath("$.authorId").value(memberId))
			.andExpect(jsonPath("$.title").value("공개 글"))
			.andExpect(jsonPath("$.contentType").value("MARKDOWN"))
			.andExpect(jsonPath("$.content").value("# 공개 글\n\n본문"))
			.andExpect(jsonPath("$.summary").value("공개 글 소개"))
			.andExpect(jsonPath("$.tags[0]").value("ddd"))
			.andExpect(jsonPath("$.tags[1]").value("tdd"))
			.andExpect(jsonPath("$.status").value("PUBLISHED"))
			.andExpect(jsonPath("$.createdAt").isString())
			.andExpect(jsonPath("$.updatedAt").isString())
			.andExpect(jsonPath("$.publishedAt").isString());
	}

	@Test
	void 회원가입_후_작성한_공개_글을_토큰_없이_목록에서_조회할_수_있다() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "이영희",
					  "nickname": "영희",
					  "loginId": "user03",
					  "password": "password123"
					}
					"""))
			.andExpect(status().isCreated());

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "user03",
					  "password": "password123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isString())
			.andReturn();

		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
			.get("accessToken")
			.asText();

		Long firstPublishedPostId = createPost(accessToken, "첫 공개 글", "PUBLISHED");
		createPost(accessToken, "임시 저장 글", "DRAFT");
		createPost(accessToken, "숨김 글", "HIDDEN");
		Long secondPublishedPostId = createPost(accessToken, "두 번째 공개 글", "PUBLISHED");
		Long memberId = jdbcTemplate.queryForObject(
			"select id from members where login_id = ?",
			Long.class,
			"user03"
		);

		mockMvc.perform(get("/api/posts")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(2))
			.andExpect(jsonPath("$.items[0].postId").value(secondPublishedPostId))
			.andExpect(jsonPath("$.items[0].authorId").value(memberId))
			.andExpect(jsonPath("$.items[0].title").value("두 번째 공개 글"))
			.andExpect(jsonPath("$.items[0].summary").value("두 번째 공개 글 소개"))
			.andExpect(jsonPath("$.items[0].tags[0]").value("ddd"))
			.andExpect(jsonPath("$.items[0].tags[1]").value("tdd"))
			.andExpect(jsonPath("$.items[0].status").value("PUBLISHED"))
			.andExpect(jsonPath("$.items[0].createdAt").isString())
			.andExpect(jsonPath("$.items[0].updatedAt").isString())
			.andExpect(jsonPath("$.items[0].publishedAt").isString())
			.andExpect(jsonPath("$.items[1].postId").value(firstPublishedPostId))
			.andExpect(jsonPath("$.items[1].status").value("PUBLISHED"))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.totalPages").value(1))
			.andExpect(jsonPath("$.hasNext").value(false));
	}

	private Long createPost(String accessToken, String title, String status) throws Exception {
		MvcResult createPostResult = mockMvc.perform(post("/api/posts")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "title": "%s",
					  "contentType": "MARKDOWN",
					  "content": "# %s\\n\\n본문",
					  "summary": "%s 소개",
					  "tags": ["TDD", "DDD"],
					  "status": "%s"
					}
					""".formatted(title, title, title, status)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.postId").isNumber())
			.andReturn();

		return objectMapper.readTree(createPostResult.getResponse().getContentAsString())
			.get("postId")
			.asLong();
	}
}
