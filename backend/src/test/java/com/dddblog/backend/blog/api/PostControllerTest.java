package com.dddblog.backend.blog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dddblog.backend.auth.security.AuthenticatedMember;
import com.dddblog.backend.auth.security.JwtAuthentication;
import com.dddblog.backend.auth.security.JwtAuthenticationEntryPoint;
import com.dddblog.backend.auth.security.JwtAuthenticationFilter;
import com.dddblog.backend.blog.application.PostNotFoundException;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.common.api.GlobalExceptionHandler;
import com.dddblog.backend.config.SecurityConfig;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WebMvcTest(PostController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class PostControllerTest {

	private static final Instant TIMESTAMP = Instant.parse("2026-06-27T10:15:30Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PostApiService postApiService;

	@MockitoBean
	private PostDetailApiService postDetailApiService;

	@MockitoBean
	private PostListApiService postListApiService;

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@BeforeEach
	void 테스트_준비() throws Exception {
		doAnswer(invocation -> {
			ServletRequest request = invocation.getArgument(0);
			ServletResponse response = invocation.getArgument(1);
			FilterChain filterChain = invocation.getArgument(2);
			filterChain.doFilter(request, response);
			return null;
		}).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
	}

	@Test
	void 인증된_요청이면_글을_생성하고_201과_글_ID를_반환한다() throws Exception {
		when(postApiService.create(eq(new MemberId(1L)), any(PostRequest.class)))
			.thenReturn(new PostResponse(10L));

		mockMvc.perform(post("/api/posts")
				.with(authentication(new JwtAuthentication(
					new AuthenticatedMember(new MemberId(1L), MemberRole.MEMBER)
				)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.postId").value(10L));
	}

	@Test
	void 인증된_회원_ID를_작성자_ID로_사용한다() throws Exception {
		when(postApiService.create(eq(new MemberId(7L)), any(PostRequest.class)))
			.thenReturn(new PostResponse(10L));

		mockMvc.perform(post("/api/posts")
				.with(authentication(new JwtAuthentication(
					new AuthenticatedMember(new MemberId(7L), MemberRole.MEMBER)
				)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isCreated());

		verify(postApiService).create(eq(new MemberId(7L)), any(PostRequest.class));
	}

	@Test
	void 요청_본문의_작성자_ID는_사용하지_않는다() throws Exception {
		when(postApiService.create(eq(new MemberId(7L)), any(PostRequest.class)))
			.thenReturn(new PostResponse(10L));

		mockMvc.perform(post("/api/posts")
				.with(authentication(new JwtAuthentication(
					new AuthenticatedMember(new MemberId(7L), MemberRole.MEMBER)
				)))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "memberId": 999,
					  "title": "DDD 시작하기",
					  "contentType": "MARKDOWN",
					  "content": "# DDD\\n\\n본문",
					  "summary": "DDD 소개",
					  "tags": ["ddd", "tdd"],
					  "status": "DRAFT"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.postId").value(10L));

		verify(postApiService).create(eq(new MemberId(7L)), any(PostRequest.class));
	}

	@Test
	void 토큰이_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Authentication failed."));
	}

	@Test
	void 인증_주체가_회원이_아니면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/posts")
				.with(authentication(new TestingAuthenticationToken("invalid", "", "ROLE_MEMBER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Authentication failed."));
	}

	@Test
	void HTML_본문_형식이면_400을_반환한다() throws Exception {
		when(postApiService.create(eq(new MemberId(1L)), any(PostRequest.class)))
			.thenThrow(new IllegalArgumentException("Post content type must be MARKDOWN."));

		mockMvc.perform(post("/api/posts")
				.with(authentication(new JwtAuthentication(
					new AuthenticatedMember(new MemberId(1L), MemberRole.MEMBER)
				)))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "title": "DDD 시작하기",
					  "contentType": "HTML",
					  "content": "<p>본문</p>",
					  "summary": "DDD 소개",
					  "tags": [],
					  "status": "DRAFT"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Post content type must be MARKDOWN."));
	}

	@Test
	void 제목이_blank이면_400을_반환한다() throws Exception {
		when(postApiService.create(eq(new MemberId(1L)), any(PostRequest.class)))
			.thenThrow(new IllegalArgumentException("Post title must not be blank."));

		mockMvc.perform(post("/api/posts")
				.with(authentication(new JwtAuthentication(
					new AuthenticatedMember(new MemberId(1L), MemberRole.MEMBER)
				)))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "title": "   ",
					  "contentType": "MARKDOWN",
					  "content": "본문",
					  "summary": "DDD 소개",
					  "tags": [],
					  "status": "DRAFT"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Post title must not be blank."));
	}

	@Test
	void 공개된_글_상세를_200으로_반환한다() throws Exception {
		when(postDetailApiService.getDetail(10L))
			.thenReturn(new PostDetailResponse(
				10L,
				1L,
				"DDD 시작하기",
				PostContentType.MARKDOWN,
				"# DDD\n\n본문",
				"DDD 소개",
				List.of("ddd", "tdd"),
				PostStatus.PUBLISHED,
				TIMESTAMP,
				TIMESTAMP,
				TIMESTAMP
			));

		mockMvc.perform(get("/api/posts/{postId}", 10L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.postId").value(10L))
			.andExpect(jsonPath("$.authorId").value(1L))
			.andExpect(jsonPath("$.title").value("DDD 시작하기"))
			.andExpect(jsonPath("$.contentType").value("MARKDOWN"))
			.andExpect(jsonPath("$.content").value("# DDD\n\n본문"))
			.andExpect(jsonPath("$.summary").value("DDD 소개"))
			.andExpect(jsonPath("$.tags[0]").value("ddd"))
			.andExpect(jsonPath("$.tags[1]").value("tdd"))
			.andExpect(jsonPath("$.status").value("PUBLISHED"))
			.andExpect(jsonPath("$.createdAt").value("2026-06-27T10:15:30Z"))
			.andExpect(jsonPath("$.updatedAt").value("2026-06-27T10:15:30Z"))
			.andExpect(jsonPath("$.publishedAt").value("2026-06-27T10:15:30Z"));
	}

	@Test
	void 공개_글_목록을_200으로_반환한다() throws Exception {
		when(postListApiService.getList(1, 10))
			.thenReturn(new PostListResponse(
				List.of(new PostListItemResponse(
					10L,
					1L,
					"DDD 시작하기",
					"DDD 소개",
					List.of("ddd", "tdd"),
					PostStatus.PUBLISHED,
					TIMESTAMP,
					TIMESTAMP,
					TIMESTAMP
				)),
				1,
				10,
				11,
				2,
				false
			));

		mockMvc.perform(get("/api/posts")
				.param("page", "1")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].postId").value(10L))
			.andExpect(jsonPath("$.items[0].authorId").value(1L))
			.andExpect(jsonPath("$.items[0].title").value("DDD 시작하기"))
			.andExpect(jsonPath("$.items[0].summary").value("DDD 소개"))
			.andExpect(jsonPath("$.items[0].tags[0]").value("ddd"))
			.andExpect(jsonPath("$.items[0].tags[1]").value("tdd"))
			.andExpect(jsonPath("$.items[0].status").value("PUBLISHED"))
			.andExpect(jsonPath("$.items[0].createdAt").value("2026-06-27T10:15:30Z"))
			.andExpect(jsonPath("$.items[0].updatedAt").value("2026-06-27T10:15:30Z"))
			.andExpect(jsonPath("$.items[0].publishedAt").value("2026-06-27T10:15:30Z"))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(10))
			.andExpect(jsonPath("$.totalElements").value(11))
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	void 토큰_없이_공개_글_목록을_조회할_수_있다() throws Exception {
		when(postListApiService.getList(null, null))
			.thenReturn(new PostListResponse(List.of(), 0, 20, 0, 0, false));

		mockMvc.perform(get("/api/posts"))
			.andExpect(status().isOk());
	}

	@Test
	void 페이지가_유효하지_않으면_400을_반환한다() throws Exception {
		when(postListApiService.getList(-1, 10))
			.thenThrow(new IllegalArgumentException("Page must be zero or positive."));

		mockMvc.perform(get("/api/posts")
				.param("page", "-1")
				.param("size", "10"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Page must be zero or positive."));
	}

	@Test
	void 페이지_크기가_유효하지_않으면_400을_반환한다() throws Exception {
		when(postListApiService.getList(0, 0))
			.thenThrow(new IllegalArgumentException("Page size must be at least 1."));

		mockMvc.perform(get("/api/posts")
				.param("page", "0")
				.param("size", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Page size must be at least 1."));
	}

	@Test
	void 토큰_없이_공개된_글_상세를_조회할_수_있다() throws Exception {
		when(postDetailApiService.getDetail(10L))
			.thenReturn(new PostDetailResponse(
				10L,
				1L,
				"DDD 시작하기",
				PostContentType.MARKDOWN,
				"# DDD\n\n본문",
				"DDD 소개",
				List.of("ddd", "tdd"),
				PostStatus.PUBLISHED,
				TIMESTAMP,
				TIMESTAMP,
				TIMESTAMP
			));

		mockMvc.perform(get("/api/posts/{postId}", 10L))
			.andExpect(status().isOk());
	}

	@Test
	void 조회할_수_없는_글이면_404를_반환한다() throws Exception {
		when(postDetailApiService.getDetail(10L))
			.thenThrow(new PostNotFoundException());

		mockMvc.perform(get("/api/posts/{postId}", 10L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Post not found."));
	}

	@Test
	void 글_ID가_유효하지_않으면_400을_반환한다() throws Exception {
		when(postDetailApiService.getDetail(0L))
			.thenThrow(new IllegalArgumentException("Post id must be positive."));

		mockMvc.perform(get("/api/posts/{postId}", 0L))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Post id must be positive."));
	}

	private String validJson() {
		return """
			{
			  "title": "DDD 시작하기",
			  "contentType": "MARKDOWN",
			  "content": "# DDD\\n\\n본문",
			  "summary": "DDD 소개",
			  "tags": ["ddd", "tdd"],
			  "status": "DRAFT"
			}
			""";
	}
}
