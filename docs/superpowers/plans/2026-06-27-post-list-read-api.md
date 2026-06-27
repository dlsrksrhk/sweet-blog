# Public Post List Read API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add domain-owned post timestamps and expose a public paginated `GET /api/posts` endpoint for `PUBLISHED` posts.

**Architecture:** First refactor the `Post` creation flow so the domain carries `createdAt`, `updatedAt`, and `publishedAt`, with `CreatePostService` supplying time from a `Clock`. Then add a separate read path for public lists through `PostListQueryService` and `PostListQueryRepository`, keeping `PostRepository` write-only. Persistence maps timestamp columns on `JpaPostEntity`; API services map read models to response records.

**Tech Stack:** Java 21, Spring Boot 3.5.0, Spring MVC, Spring Security, Spring Data JPA, MySQL Testcontainers, JUnit 5, AssertJ, Mockito, MockMvc

---

## File Structure

- Modify: `backend/src/main/java/com/dddblog/backend/blog/domain/Post.java`
  - Add timestamp fields and validation rules.
- Modify: `backend/src/test/java/com/dddblog/backend/blog/domain/PostTest.java`
  - Cover timestamp invariants and update existing constructor calls.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostService.java`
  - Inject `Clock`, calculate `createdAt`, `updatedAt`, and `publishedAt`.
- Modify: `backend/src/test/java/com/dddblog/backend/blog/application/CreatePostServiceTest.java`
  - Use a fixed clock and verify timestamp behavior.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java`
  - Wire `CreatePostService` with the existing `Clock` bean and expose `PostListQueryService`.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/application/PostDetail.java`
  - Add timestamp fields to the detail read model.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/api/PostDetailResponse.java`
  - Add timestamp fields to public detail response.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/api/PostDetailApiService.java`
  - Map timestamp fields.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostEntity.java`
  - Map `created_at`, `updated_at`, `published_at`.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapter.java`
  - Persist timestamps from domain `Post`.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostDetailQueryRepositoryAdapter.java`
  - Map timestamps into `PostDetail`.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaPostRepository.java`
  - Add page query method for published posts.
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostListQuery.java`
  - Holds `page` and `size`.
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostListItem.java`
  - Public post list item read model.
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostListPage.java`
  - Public post list page read model.
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostListQueryRepository.java`
  - Application port for public list reads.
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostListQueryService.java`
  - Validates pagination and delegates to the port.
- Create: `backend/src/test/java/com/dddblog/backend/blog/application/FakePostListQueryRepository.java`
  - Test fake for list query service/API service tests.
- Create: `backend/src/test/java/com/dddblog/backend/blog/application/PostListQueryServiceTest.java`
  - Pure application tests for pagination and delegation.
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostListQueryRepositoryAdapter.java`
  - JPA adapter for public post lists.
- Create: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostListQueryRepositoryAdapterTest.java`
  - MySQL Testcontainers tests for filtering, sorting, pagination, tag order.
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostListItemResponse.java`
  - HTTP item response.
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostListResponse.java`
  - HTTP page response.
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostListApiService.java`
  - Applies API defaults and maps application page to HTTP response.
- Create: `backend/src/test/java/com/dddblog/backend/blog/api/PostListApiServiceTest.java`
  - API service mapping/default tests.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/api/PostController.java`
  - Add `GET /api/posts`.
- Modify: `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`
  - Permit public `GET /api/posts`.
- Modify: existing API/persistence/integration tests where timestamp fields are now required.

---

### Task 1: Add Domain Timestamp Model To Post

**Files:**
- Modify: `backend/src/test/java/com/dddblog/backend/blog/domain/PostTest.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/domain/Post.java`

- [ ] **Step 1: Update `PostTest` with timestamp expectations and helper**

In `backend/src/test/java/com/dddblog/backend/blog/domain/PostTest.java`, add the import:

```java
import java.time.Instant;
```

Add constants inside the class:

```java
	private static final Instant CREATED_AT = Instant.parse("2026-06-27T10:15:30Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-27T10:15:30Z");
	private static final Instant PUBLISHED_AT = Instant.parse("2026-06-27T10:15:30Z");
```

Update the existing `글을_생성한다` test to create a draft with timestamps and assert them:

```java
		Post post = new Post(
			authorId,
			title,
			content,
			PostContentType.MARKDOWN,
			summary,
			tags,
			PostStatus.DRAFT,
			CREATED_AT,
			UPDATED_AT,
			null
		);

		assertThat(post.authorId()).isEqualTo(authorId);
		assertThat(post.title()).isEqualTo(title);
		assertThat(post.content()).isEqualTo(content);
		assertThat(post.contentType()).isEqualTo(PostContentType.MARKDOWN);
		assertThat(post.summary()).isEqualTo(summary);
		assertThat(post.tags()).containsExactlyElementsOf(tags);
		assertThat(post.status()).isEqualTo(PostStatus.DRAFT);
		assertThat(post.createdAt()).isEqualTo(CREATED_AT);
		assertThat(post.updatedAt()).isEqualTo(UPDATED_AT);
		assertThat(post.publishedAt()).isNull();
```

Add these failing timestamp tests:

```java
	@Test
	void 공개_글을_생성하면_발행일을_가진다() {
		Post post = createPost(PostStatus.PUBLISHED, PUBLISHED_AT);

		assertThat(post.publishedAt()).isEqualTo(PUBLISHED_AT);
	}

	@Test
	void 작성일이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT,
			null,
			UPDATED_AT,
			null
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post created at must not be null.");
	}

	@Test
	void 수정일이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT,
			CREATED_AT,
			null,
			null
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post updated at must not be null.");
	}

	@Test
	void 수정일이_작성일보다_이전이면_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT,
			CREATED_AT,
			CREATED_AT.minusSeconds(1),
			null
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post updated at must not be before created at.");
	}

	@Test
	void 공개_글의_발행일이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> createPost(PostStatus.PUBLISHED, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post published at must not be null when published.");
	}

	@Test
	void 발행일이_작성일보다_이전이면_생성할_수_없다() {
		assertThatThrownBy(() -> createPost(PostStatus.PUBLISHED, CREATED_AT.minusSeconds(1)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post published at must not be before created at.");
	}
```

Add this helper at the bottom of the test class:

```java
	private Post createPost(PostStatus status, Instant publishedAt) {
		return new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			List.of(),
			status,
			CREATED_AT,
			UPDATED_AT,
			publishedAt
		);
	}
```

Update every existing `new Post(...)` in `PostTest` to pass `CREATED_AT`, `UPDATED_AT`, and a status-compatible `publishedAt`:

```java
PostStatus.DRAFT,
CREATED_AT,
UPDATED_AT,
null
```

or, for `PostStatus.PUBLISHED`:

```java
PostStatus.PUBLISHED,
CREATED_AT,
UPDATED_AT,
PUBLISHED_AT
```

- [ ] **Step 2: Run domain test to verify it fails**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostTest
```

Expected:

- `BUILD FAILED`
- Compilation errors mention the old `Post` constructor or missing timestamp accessors.

- [ ] **Step 3: Update `Post` with timestamp fields**

Replace `backend/src/main/java/com/dddblog/backend/blog/domain/Post.java` with:

```java
package com.dddblog.backend.blog.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

public final class Post {

	private static final int MAX_TAG_COUNT = 10;

	private final AuthorId authorId;
	private final PostTitle title;
	private final PostContent content;
	private final PostContentType contentType;
	private final PostSummary summary;
	private final List<TagName> tags;
	private final PostStatus status;
	private final Instant createdAt;
	private final Instant updatedAt;
	private final Instant publishedAt;

	public Post(
		AuthorId authorId,
		PostTitle title,
		PostContent content,
		PostContentType contentType,
		PostSummary summary,
		List<TagName> tags,
		PostStatus status,
		Instant createdAt,
		Instant updatedAt,
		Instant publishedAt
	) {
		if (authorId == null) {
			throw new IllegalArgumentException("Post author must not be null.");
		}
		if (title == null) {
			throw new IllegalArgumentException("Post title must not be null.");
		}
		if (content == null) {
			throw new IllegalArgumentException("Post content must not be null.");
		}
		if (contentType == null) {
			throw new IllegalArgumentException("Post content type must not be null.");
		}
		if (status == null) {
			throw new IllegalArgumentException("Post status must not be null.");
		}
		if (createdAt == null) {
			throw new IllegalArgumentException("Post created at must not be null.");
		}
		if (updatedAt == null) {
			throw new IllegalArgumentException("Post updated at must not be null.");
		}
		if (updatedAt.isBefore(createdAt)) {
			throw new IllegalArgumentException("Post updated at must not be before created at.");
		}
		if (status == PostStatus.PUBLISHED && publishedAt == null) {
			throw new IllegalArgumentException("Post published at must not be null when published.");
		}
		if (publishedAt != null && publishedAt.isBefore(createdAt)) {
			throw new IllegalArgumentException("Post published at must not be before created at.");
		}
		List<TagName> copiedTags = copyTags(tags);
		if (copiedTags.size() > MAX_TAG_COUNT) {
			throw new IllegalArgumentException("Post tags must be 10 or less.");
		}
		this.authorId = authorId;
		this.title = title;
		this.content = content;
		this.contentType = contentType;
		this.summary = summary == null ? new PostSummary(null) : summary;
		this.tags = copiedTags;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.publishedAt = publishedAt;
	}

	private List<TagName> copyTags(List<TagName> tags) {
		if (tags == null) {
			return List.of();
		}
		if (tags.stream().anyMatch(tag -> tag == null)) {
			throw new IllegalArgumentException("Post tag must not be null.");
		}
		validateDuplicatedTags(tags);
		return List.copyOf(tags);
	}

	private void validateDuplicatedTags(List<TagName> tags) {
		if (new HashSet<>(tags).size() != tags.size()) {
			throw new IllegalArgumentException("Post tags must not be duplicated.");
		}
	}

	public AuthorId authorId() {
		return authorId;
	}

	public PostTitle title() {
		return title;
	}

	public PostContent content() {
		return content;
	}

	public PostContentType contentType() {
		return contentType;
	}

	public PostSummary summary() {
		return summary;
	}

	public List<TagName> tags() {
		return tags;
	}

	public PostStatus status() {
		return status;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public Instant publishedAt() {
		return publishedAt;
	}
}
```

- [ ] **Step 4: Run domain test**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostTest
```

Expected:

- `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit domain timestamp model**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/main/java/com/dddblog/backend/blog/domain/Post.java `
  backend/src/test/java/com/dddblog/backend/blog/domain/PostTest.java
git commit -m "feat: add post timestamp rules"
```

Expected:

- Commit succeeds.

---

### Task 2: Supply Post Timestamps From CreatePostService

**Files:**
- Modify: `backend/src/test/java/com/dddblog/backend/blog/application/CreatePostServiceTest.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostService.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java`
- Modify call sites in tests that instantiate `CreatePostService`

- [ ] **Step 1: Update create post service tests for a fixed clock**

In `backend/src/test/java/com/dddblog/backend/blog/application/CreatePostServiceTest.java`, add imports:

```java
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
```

Add constants inside the class:

```java
	private static final Instant NOW = Instant.parse("2026-06-27T10:15:30Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
```

Replace each service construction:

```java
CreatePostService service = new CreatePostService(postRepository);
```

with:

```java
CreatePostService service = new CreatePostService(postRepository, CLOCK);
```

Add these failing tests:

```java
	@Test
	void 글을_생성하면_현재_시각을_작성일과_수정일로_저장한다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository, CLOCK);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			PostContentType.MARKDOWN,
			"본문",
			"요약",
			List.of(),
			PostStatus.DRAFT
		);

		service.create(command);

		Post savedPost = postRepository.savedPosts().get(0);
		assertThat(savedPost.createdAt()).isEqualTo(NOW);
		assertThat(savedPost.updatedAt()).isEqualTo(NOW);
	}

	@Test
	void 공개_글을_생성하면_현재_시각을_발행일로_저장한다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository, CLOCK);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			PostContentType.MARKDOWN,
			"본문",
			"요약",
			List.of(),
			PostStatus.PUBLISHED
		);

		service.create(command);

		assertThat(postRepository.savedPosts().get(0).publishedAt()).isEqualTo(NOW);
	}

	@Test
	void 임시_저장_글을_생성하면_발행일을_저장하지_않는다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository, CLOCK);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			PostContentType.MARKDOWN,
			"본문",
			"요약",
			List.of(),
			PostStatus.DRAFT
		);

		service.create(command);

		assertThat(postRepository.savedPosts().get(0).publishedAt()).isNull();
	}
```

- [ ] **Step 2: Run create post service tests to verify failure**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.application.CreatePostServiceTest
```

Expected:

- `BUILD FAILED`
- Compilation errors mention the missing `CreatePostService(PostRepository, Clock)` constructor.

- [ ] **Step 3: Update `CreatePostService`**

Replace `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostService.java` with:

```java
package com.dddblog.backend.blog.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

public class CreatePostService {

	private final PostRepository postRepository;
	private final Clock clock;

	public CreatePostService(PostRepository postRepository, Clock clock) {
		this.postRepository = postRepository;
		this.clock = clock;
	}

	public PostId create(CreatePostCommand command) {
		if (command == null) {
			throw new IllegalArgumentException("Create post command must not be null.");
		}
		Instant now = Instant.now(clock);
		Post post = new Post(
			new AuthorId(command.authorId()),
			new PostTitle(command.title()),
			new PostContent(command.content()),
			command.contentType(),
			new PostSummary(command.summary()),
			toTagNames(command.tags()),
			command.status(),
			now,
			now,
			publishedAt(command.status(), now)
		);
		return postRepository.save(post);
	}

	private Instant publishedAt(PostStatus status, Instant now) {
		if (status == PostStatus.PUBLISHED) {
			return now;
		}
		return null;
	}

	private List<TagName> toTagNames(List<String> tags) {
		if (tags == null) {
			return List.of();
		}
		return tags.stream()
			.map(TagName::new)
			.toList();
	}
}
```

- [ ] **Step 4: Update `BlogApplicationConfig` wiring**

Modify `backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java`:

```java
package com.dddblog.backend.blog.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dddblog.backend.blog.application.CreatePostService;
import com.dddblog.backend.blog.application.PostDetailQueryRepository;
import com.dddblog.backend.blog.application.PostDetailQueryService;
import com.dddblog.backend.blog.application.PostRepository;

@Configuration
public class BlogApplicationConfig {

	@Bean
	CreatePostService createPostService(PostRepository postRepository, Clock clock) {
		return new CreatePostService(postRepository, clock);
	}

	@Bean
	PostDetailQueryService postDetailQueryService(PostDetailQueryRepository postDetailQueryRepository) {
		return new PostDetailQueryService(postDetailQueryRepository);
	}
}
```

- [ ] **Step 5: Run create post service tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.application.CreatePostServiceTest
```

Expected:

- `BUILD SUCCESSFUL`

- [ ] **Step 6: Compile tests to find old `Post` constructor call sites**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat compileTestJava
```

Expected:

- If compilation fails, update each old `new Post(...)` call site by adding status-compatible timestamps, using `Instant.parse("2026-06-27T10:15:30Z")` in tests.
- Re-run until `compileTestJava` succeeds.

- [ ] **Step 7: Commit create flow timestamp wiring**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/main/java/com/dddblog/backend/blog/application/CreatePostService.java `
  backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java `
  backend/src/test/java/com/dddblog/backend/blog/application/CreatePostServiceTest.java `
  backend/src/test/java/com/dddblog/backend
git commit -m "feat: timestamp created posts from clock"
```

Expected:

- Commit succeeds.

---

### Task 3: Persist And Return Timestamp Fields In Detail Reads

**Files:**
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostEntity.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapter.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostDetailQueryRepositoryAdapter.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/application/PostDetail.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/api/PostDetailResponse.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/api/PostDetailApiService.java`
- Modify related persistence/API tests.

- [ ] **Step 1: Extend persistence and detail tests first**

In `JpaPostRepositoryAdapterTest`, assert saved timestamp columns:

```java
		assertThat(savedPost.createdAt()).isEqualTo(CREATED_AT);
		assertThat(savedPost.updatedAt()).isEqualTo(UPDATED_AT);
		assertThat(savedPost.publishedAt()).isNull();
```

In `JpaPostDetailQueryRepositoryAdapterTest`, assert detail timestamps for a published post:

```java
		assertThat(postDetail.createdAt()).isEqualTo(CREATED_AT);
		assertThat(postDetail.updatedAt()).isEqualTo(UPDATED_AT);
		assertThat(postDetail.publishedAt()).isEqualTo(PUBLISHED_AT);
```

In `PostDetailApiServiceTest`, expect `PostDetailResponse` to contain:

```java
assertThat(response.createdAt()).isEqualTo(CREATED_AT);
assertThat(response.updatedAt()).isEqualTo(UPDATED_AT);
assertThat(response.publishedAt()).isEqualTo(PUBLISHED_AT);
```

In `PostControllerTest`, update `PostDetailResponse` construction to include timestamps:

```java
Instant.parse("2026-06-27T10:15:30Z"),
Instant.parse("2026-06-27T10:15:30Z"),
Instant.parse("2026-06-27T10:15:30Z")
```

and add JSON assertions:

```java
			.andExpect(jsonPath("$.createdAt").value("2026-06-27T10:15:30Z"))
			.andExpect(jsonPath("$.updatedAt").value("2026-06-27T10:15:30Z"))
			.andExpect(jsonPath("$.publishedAt").value("2026-06-27T10:15:30Z"));
```

- [ ] **Step 2: Run focused tests to verify failure**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.persistence.JpaPostRepositoryAdapterTest --tests com.dddblog.backend.blog.persistence.JpaPostDetailQueryRepositoryAdapterTest --tests com.dddblog.backend.blog.api.PostDetailApiServiceTest --tests com.dddblog.backend.blog.api.PostControllerTest
```

Expected:

- `BUILD FAILED`
- Failures mention missing timestamp fields/accessors.

- [ ] **Step 3: Add timestamp fields to `JpaPostEntity`**

Add import:

```java
import java.time.Instant;
```

Add fields:

```java
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "published_at")
	private Instant publishedAt;
```

Update constructor signature and assignments:

```java
	JpaPostEntity(
		Long authorId,
		String title,
		PostContentType contentType,
		String contentMarkdown,
		String summary,
		PostStatus status,
		Instant createdAt,
		Instant updatedAt,
		Instant publishedAt,
		Set<JpaTagEntity> tags
	) {
		this.authorId = authorId;
		this.title = title;
		this.contentType = contentType;
		this.contentMarkdown = contentMarkdown;
		this.summary = summary;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.publishedAt = publishedAt;
		this.tags = new LinkedHashSet<>(tags);
	}
```

Add accessors:

```java
	Instant createdAt() {
		return createdAt;
	}

	Instant updatedAt() {
		return updatedAt;
	}

	Instant publishedAt() {
		return publishedAt;
	}
```

- [ ] **Step 4: Persist timestamps in `JpaPostRepositoryAdapter`**

Update `new JpaPostEntity(...)` call:

```java
		JpaPostEntity entity = new JpaPostEntity(
			post.authorId().value(),
			post.title().value(),
			post.contentType(),
			post.content().value(),
			post.summary().value(),
			post.status(),
			post.createdAt(),
			post.updatedAt(),
			post.publishedAt(),
			tags
		);
```

- [ ] **Step 5: Add timestamps to `PostDetail`**

Add import:

```java
import java.time.Instant;
```

Update record fields:

```java
public record PostDetail(
	PostId postId,
	AuthorId authorId,
	PostTitle title,
	PostContentType contentType,
	PostContent content,
	PostSummary summary,
	List<TagName> tags,
	PostStatus status,
	Instant createdAt,
	Instant updatedAt,
	Instant publishedAt
)
```

Add compact constructor validation:

```java
		if (createdAt == null) {
			throw new IllegalArgumentException("Post created at must not be null.");
		}
		if (updatedAt == null) {
			throw new IllegalArgumentException("Post updated at must not be null.");
		}
		if (publishedAt == null) {
			throw new IllegalArgumentException("Post published at must not be null when published.");
		}
```

The public detail query only returns `PUBLISHED` posts, so `publishedAt` must be present in this read model.

- [ ] **Step 6: Map timestamps in `JpaPostDetailQueryRepositoryAdapter`**

Update `new PostDetail(...)`:

```java
		return new PostDetail(
			new PostId(entity.id()),
			new AuthorId(entity.authorId()),
			new PostTitle(entity.title()),
			entity.contentType(),
			new PostContent(entity.contentMarkdown()),
			new PostSummary(entity.summary()),
			toTagNames(entity),
			entity.status(),
			entity.createdAt(),
			entity.updatedAt(),
			entity.publishedAt()
		);
```

- [ ] **Step 7: Add timestamps to detail API response**

In `PostDetailResponse`, add:

```java
import java.time.Instant;
```

and extend the record:

```java
public record PostDetailResponse(
	Long postId,
	Long authorId,
	String title,
	PostContentType contentType,
	String content,
	String summary,
	List<String> tags,
	PostStatus status,
	Instant createdAt,
	Instant updatedAt,
	Instant publishedAt
) {
}
```

Update `PostDetailApiService` response construction:

```java
			postDetail.status(),
			postDetail.createdAt(),
			postDetail.updatedAt(),
			postDetail.publishedAt()
```

- [ ] **Step 8: Run focused tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.persistence.JpaPostRepositoryAdapterTest --tests com.dddblog.backend.blog.persistence.JpaPostDetailQueryRepositoryAdapterTest --tests com.dddblog.backend.blog.api.PostDetailApiServiceTest --tests com.dddblog.backend.blog.api.PostControllerTest
```

Expected:

- `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit persistence/detail timestamp support**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostEntity.java `
  backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapter.java `
  backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostDetailQueryRepositoryAdapter.java `
  backend/src/main/java/com/dddblog/backend/blog/application/PostDetail.java `
  backend/src/main/java/com/dddblog/backend/blog/api/PostDetailResponse.java `
  backend/src/main/java/com/dddblog/backend/blog/api/PostDetailApiService.java `
  backend/src/test/java/com/dddblog/backend/blog
git commit -m "feat: expose post timestamps in detail reads"
```

Expected:

- Commit succeeds.

---

### Task 4: Add Public Post List Application Query Service

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostListQuery.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostListItem.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostListPage.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostListQueryRepository.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostListQueryService.java`
- Create: `backend/src/test/java/com/dddblog/backend/blog/application/FakePostListQueryRepository.java`
- Create: `backend/src/test/java/com/dddblog/backend/blog/application/PostListQueryServiceTest.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java`

- [ ] **Step 1: Write failing application tests**

Create `backend/src/test/java/com/dddblog/backend/blog/application/FakePostListQueryRepository.java`:

```java
package com.dddblog.backend.blog.application;

public class FakePostListQueryRepository implements PostListQueryRepository {

	private PostListPage postListPage = new PostListPage(
		java.util.List.of(),
		0,
		20,
		0,
		0,
		false
	);
	private PostListQuery lastQuery;

	@Override
	public PostListPage findPublished(PostListQuery query) {
		this.lastQuery = query;
		return postListPage;
	}

	public void 결과를_준비한다(PostListPage postListPage) {
		this.postListPage = postListPage;
	}

	public PostListQuery 마지막_조회_조건() {
		return lastQuery;
	}
}
```

Create `backend/src/test/java/com/dddblog/backend/blog/application/PostListQueryServiceTest.java`:

```java
package com.dddblog.backend.blog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

class PostListQueryServiceTest {

	private static final Instant NOW = Instant.parse("2026-06-27T10:15:30Z");

	@Test
	void 공개_글_목록을_조회한다() {
		FakePostListQueryRepository repository = new FakePostListQueryRepository();
		PostListPage page = new PostListPage(
			List.of(createItem()),
			0,
			20,
			1,
			1,
			false
		);
		repository.결과를_준비한다(page);
		PostListQueryService service = new PostListQueryService(repository);

		PostListPage result = service.getList(new PostListQuery(0, 20));

		assertThat(result).isEqualTo(page);
		assertThat(repository.마지막_조회_조건()).isEqualTo(new PostListQuery(0, 20));
	}

	@Test
	void 조회_조건이_null이면_조회할_수_없다() {
		PostListQueryService service = new PostListQueryService(new FakePostListQueryRepository());

		assertThatThrownBy(() -> service.getList(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post list query must not be null.");
	}

	@Test
	void 페이지가_음수이면_조회할_수_없다() {
		PostListQueryService service = new PostListQueryService(new FakePostListQueryRepository());

		assertThatThrownBy(() -> service.getList(new PostListQuery(-1, 20)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Page must be zero or positive.");
	}

	@Test
	void 페이지_크기가_1보다_작으면_조회할_수_없다() {
		PostListQueryService service = new PostListQueryService(new FakePostListQueryRepository());

		assertThatThrownBy(() -> service.getList(new PostListQuery(0, 0)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Page size must be at least 1.");
	}

	@Test
	void 페이지_크기가_50보다_크면_조회할_수_없다() {
		PostListQueryService service = new PostListQueryService(new FakePostListQueryRepository());

		assertThatThrownBy(() -> service.getList(new PostListQuery(0, 51)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Page size must be 50 or less.");
	}

	private PostListItem createItem() {
		return new PostListItem(
			new PostId(1L),
			new AuthorId(10L),
			new PostTitle("DDD 시작하기"),
			new PostSummary("DDD 소개"),
			List.of(new TagName("ddd"), new TagName("tdd")),
			PostStatus.PUBLISHED,
			NOW,
			NOW,
			NOW
		);
	}
}
```

- [ ] **Step 2: Run application list tests to verify failure**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.application.PostListQueryServiceTest
```

Expected:

- `BUILD FAILED`
- Compilation errors mention missing list query classes.

- [ ] **Step 3: Add list application classes**

Create `backend/src/main/java/com/dddblog/backend/blog/application/PostListQuery.java`:

```java
package com.dddblog.backend.blog.application;

public record PostListQuery(
	int page,
	int size
) {
}
```

Create `backend/src/main/java/com/dddblog/backend/blog/application/PostListItem.java`:

```java
package com.dddblog.backend.blog.application;

import java.time.Instant;
import java.util.List;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

public record PostListItem(
	PostId postId,
	AuthorId authorId,
	PostTitle title,
	PostSummary summary,
	List<TagName> tags,
	PostStatus status,
	Instant createdAt,
	Instant updatedAt,
	Instant publishedAt
) {

	public PostListItem {
		if (postId == null) {
			throw new IllegalArgumentException("Post id must not be null.");
		}
		if (authorId == null) {
			throw new IllegalArgumentException("Post author must not be null.");
		}
		if (title == null) {
			throw new IllegalArgumentException("Post title must not be null.");
		}
		if (summary == null) {
			summary = new PostSummary(null);
		}
		if (tags == null) {
			tags = List.of();
		}
		if (tags.stream().anyMatch(tag -> tag == null)) {
			throw new IllegalArgumentException("Post tag must not be null.");
		}
		if (status == null) {
			throw new IllegalArgumentException("Post status must not be null.");
		}
		if (createdAt == null) {
			throw new IllegalArgumentException("Post created at must not be null.");
		}
		if (updatedAt == null) {
			throw new IllegalArgumentException("Post updated at must not be null.");
		}
		if (publishedAt == null) {
			throw new IllegalArgumentException("Post published at must not be null when published.");
		}
		tags = List.copyOf(tags);
	}
}
```

Create `backend/src/main/java/com/dddblog/backend/blog/application/PostListPage.java`:

```java
package com.dddblog.backend.blog.application;

import java.util.List;

public record PostListPage(
	List<PostListItem> items,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {

	public PostListPage {
		if (items == null) {
			items = List.of();
		}
		items = List.copyOf(items);
	}
}
```

Create `backend/src/main/java/com/dddblog/backend/blog/application/PostListQueryRepository.java`:

```java
package com.dddblog.backend.blog.application;

public interface PostListQueryRepository {

	PostListPage findPublished(PostListQuery query);
}
```

Create `backend/src/main/java/com/dddblog/backend/blog/application/PostListQueryService.java`:

```java
package com.dddblog.backend.blog.application;

public class PostListQueryService {

	private static final int MAX_PAGE_SIZE = 50;

	private final PostListQueryRepository postListQueryRepository;

	public PostListQueryService(PostListQueryRepository postListQueryRepository) {
		this.postListQueryRepository = postListQueryRepository;
	}

	public PostListPage getList(PostListQuery query) {
		if (query == null) {
			throw new IllegalArgumentException("Post list query must not be null.");
		}
		if (query.page() < 0) {
			throw new IllegalArgumentException("Page must be zero or positive.");
		}
		if (query.size() < 1) {
			throw new IllegalArgumentException("Page size must be at least 1.");
		}
		if (query.size() > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Page size must be 50 or less.");
		}
		return postListQueryRepository.findPublished(query);
	}
}
```

- [ ] **Step 4: Wire list query service**

Update `BlogApplicationConfig` imports and bean:

```java
import com.dddblog.backend.blog.application.PostListQueryRepository;
import com.dddblog.backend.blog.application.PostListQueryService;
```

```java
	@Bean
	PostListQueryService postListQueryService(PostListQueryRepository postListQueryRepository) {
		return new PostListQueryService(postListQueryRepository);
	}
```

- [ ] **Step 5: Run application list tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.application.PostListQueryServiceTest
```

Expected:

- `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit list application layer**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/main/java/com/dddblog/backend/blog/application/PostListQuery.java `
  backend/src/main/java/com/dddblog/backend/blog/application/PostListItem.java `
  backend/src/main/java/com/dddblog/backend/blog/application/PostListPage.java `
  backend/src/main/java/com/dddblog/backend/blog/application/PostListQueryRepository.java `
  backend/src/main/java/com/dddblog/backend/blog/application/PostListQueryService.java `
  backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java `
  backend/src/test/java/com/dddblog/backend/blog/application/FakePostListQueryRepository.java `
  backend/src/test/java/com/dddblog/backend/blog/application/PostListQueryServiceTest.java
git commit -m "feat: add post list query service"
```

Expected:

- Commit succeeds.

---

### Task 5: Add JPA Public Post List Query Adapter

**Files:**
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaPostRepository.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostListQueryRepositoryAdapter.java`
- Create: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostListQueryRepositoryAdapterTest.java`

- [ ] **Step 1: Write failing JPA adapter tests**

Create `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostListQueryRepositoryAdapterTest.java` with tests for published-only filtering, sorting, pagination, and tag order:

```java
package com.dddblog.backend.blog.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.dddblog.backend.blog.application.PostListPage;
import com.dddblog.backend.blog.application.PostListQuery;
import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;
import com.dddblog.backend.support.MysqlDataJpaTestSupport;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaPostRepositoryAdapter.class, JpaPostListQueryRepositoryAdapter.class})
class JpaPostListQueryRepositoryAdapterTest extends MysqlDataJpaTestSupport {

	private static final Instant FIRST = Instant.parse("2026-06-27T10:15:30Z");
	private static final Instant SECOND = Instant.parse("2026-06-27T10:16:30Z");

	@Autowired
	private JpaPostRepositoryAdapter postRepository;

	@Autowired
	private JpaPostListQueryRepositoryAdapter postListQueryRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 공개된_글_목록을_조회한다() {
		PostId postId = postRepository.save(createPost("공개 글", PostStatus.PUBLISHED, FIRST));
		entityManager.flush();
		entityManager.clear();

		PostListPage page = postListQueryRepository.findPublished(new PostListQuery(0, 20));

		assertThat(page.items()).hasSize(1);
		assertThat(page.items().get(0).postId()).isEqualTo(postId);
		assertThat(page.items().get(0).title().value()).isEqualTo("공개 글");
		assertThat(page.items().get(0).publishedAt()).isEqualTo(FIRST);
		assertThat(page.totalElements()).isEqualTo(1);
		assertThat(page.totalPages()).isEqualTo(1);
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void 임시_저장_글과_숨김_글은_목록에_포함하지_않는다() {
		postRepository.save(createPost("공개 글", PostStatus.PUBLISHED, FIRST));
		postRepository.save(createPost("임시 글", PostStatus.DRAFT, null));
		postRepository.save(createPost("숨김 글", PostStatus.HIDDEN, null));
		entityManager.flush();
		entityManager.clear();

		PostListPage page = postListQueryRepository.findPublished(new PostListQuery(0, 20));

		assertThat(page.items()).extracting(item -> item.title().value()).containsExactly("공개 글");
	}

	@Test
	void 발행일_내림차순으로_조회한다() {
		postRepository.save(createPost("먼저 공개", PostStatus.PUBLISHED, FIRST));
		postRepository.save(createPost("나중 공개", PostStatus.PUBLISHED, SECOND));
		entityManager.flush();
		entityManager.clear();

		PostListPage page = postListQueryRepository.findPublished(new PostListQuery(0, 20));

		assertThat(page.items()).extracting(item -> item.title().value()).containsExactly("나중 공개", "먼저 공개");
	}

	@Test
	void 페이지와_크기를_적용해_조회한다() {
		postRepository.save(createPost("첫 번째", PostStatus.PUBLISHED, FIRST));
		postRepository.save(createPost("두 번째", PostStatus.PUBLISHED, SECOND));
		entityManager.flush();
		entityManager.clear();

		PostListPage page = postListQueryRepository.findPublished(new PostListQuery(0, 1));

		assertThat(page.items()).hasSize(1);
		assertThat(page.items().get(0).title().value()).isEqualTo("두 번째");
		assertThat(page.page()).isEqualTo(0);
		assertThat(page.size()).isEqualTo(1);
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.totalPages()).isEqualTo(2);
		assertThat(page.hasNext()).isTrue();
	}

	@Test
	void 태그를_이름_오름차순으로_조회한다() {
		postRepository.save(createPost("공개 글", PostStatus.PUBLISHED, FIRST));
		entityManager.flush();
		entityManager.clear();

		PostListPage page = postListQueryRepository.findPublished(new PostListQuery(0, 20));

		assertThat(page.items().get(0).tags()).extracting(TagName::value).containsExactly("ddd", "tdd");
	}

	private Post createPost(String title, PostStatus status, Instant publishedAt) {
		return new Post(
			new AuthorId(1L),
			new PostTitle(title),
			new PostContent("# 본문"),
			PostContentType.MARKDOWN,
			new PostSummary("요약"),
			List.of(new TagName("tdd"), new TagName("ddd")),
			status,
			FIRST,
			FIRST,
			publishedAt
		);
	}
}
```

- [ ] **Step 2: Run JPA list tests to verify failure**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.persistence.JpaPostListQueryRepositoryAdapterTest
```

Expected:

- `BUILD FAILED`
- Compilation errors mention missing `JpaPostListQueryRepositoryAdapter`.

- [ ] **Step 3: Add Spring Data page query**

Modify `SpringDataJpaPostRepository`:

```java
package com.dddblog.backend.blog.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dddblog.backend.blog.domain.PostStatus;

interface SpringDataJpaPostRepository extends JpaRepository<JpaPostEntity, Long> {

	Optional<JpaPostEntity> findByIdAndStatus(Long id, PostStatus status);

	Page<JpaPostEntity> findByStatus(PostStatus status, Pageable pageable);
}
```

- [ ] **Step 4: Create JPA list adapter**

Create `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostListQueryRepositoryAdapter.java`:

```java
package com.dddblog.backend.blog.persistence;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dddblog.backend.blog.application.PostListItem;
import com.dddblog.backend.blog.application.PostListPage;
import com.dddblog.backend.blog.application.PostListQuery;
import com.dddblog.backend.blog.application.PostListQueryRepository;
import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

@Repository
public class JpaPostListQueryRepositoryAdapter implements PostListQueryRepository {

	private final SpringDataJpaPostRepository postRepository;

	public JpaPostListQueryRepositoryAdapter(SpringDataJpaPostRepository postRepository) {
		this.postRepository = postRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public PostListPage findPublished(PostListQuery query) {
		PageRequest pageRequest = PageRequest.of(
			query.page(),
			query.size(),
			Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))
		);
		Page<JpaPostEntity> page = postRepository.findByStatus(PostStatus.PUBLISHED, pageRequest);
		return new PostListPage(
			page.getContent().stream()
				.map(this::toPostListItem)
				.toList(),
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages(),
			page.hasNext()
		);
	}

	private PostListItem toPostListItem(JpaPostEntity entity) {
		return new PostListItem(
			new PostId(entity.id()),
			new AuthorId(entity.authorId()),
			new PostTitle(entity.title()),
			new PostSummary(entity.summary()),
			toTagNames(entity),
			entity.status(),
			entity.createdAt(),
			entity.updatedAt(),
			entity.publishedAt()
		);
	}

	private List<TagName> toTagNames(JpaPostEntity entity) {
		return entity.tags().stream()
			.map(JpaTagEntity::name)
			.sorted(Comparator.naturalOrder())
			.map(TagName::new)
			.toList();
	}
}
```

- [ ] **Step 5: Run JPA list tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.persistence.JpaPostListQueryRepositoryAdapterTest
```

Expected:

- `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit JPA list adapter**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaPostRepository.java `
  backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostListQueryRepositoryAdapter.java `
  backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostListQueryRepositoryAdapterTest.java
git commit -m "feat: add public post list query adapter"
```

Expected:

- Commit succeeds.

---

### Task 6: Add Public Post List API Endpoint

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostListItemResponse.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostListResponse.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostListApiService.java`
- Create: `backend/src/test/java/com/dddblog/backend/blog/api/PostListApiServiceTest.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/api/PostController.java`
- Modify: `backend/src/test/java/com/dddblog/backend/blog/api/PostControllerTest.java`
- Modify: `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`

- [ ] **Step 1: Write failing API service test**

Create `backend/src/test/java/com/dddblog/backend/blog/api/PostListApiServiceTest.java`:

```java
package com.dddblog.backend.blog.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.blog.application.FakePostListQueryRepository;
import com.dddblog.backend.blog.application.PostListItem;
import com.dddblog.backend.blog.application.PostListPage;
import com.dddblog.backend.blog.application.PostListQueryService;
import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

class PostListApiServiceTest {

	private static final Instant NOW = Instant.parse("2026-06-27T10:15:30Z");

	@Test
	void 공개_글_목록_응답으로_변환한다() {
		FakePostListQueryRepository repository = new FakePostListQueryRepository();
		repository.결과를_준비한다(new PostListPage(
			List.of(createItem()),
			0,
			20,
			1,
			1,
			false
		));
		PostListApiService service = new PostListApiService(new PostListQueryService(repository));

		PostListResponse response = service.getList(0, 20);

		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).postId()).isEqualTo(1L);
		assertThat(response.items().get(0).authorId()).isEqualTo(10L);
		assertThat(response.items().get(0).title()).isEqualTo("DDD 시작하기");
		assertThat(response.items().get(0).summary()).isEqualTo("DDD 소개");
		assertThat(response.items().get(0).tags()).containsExactly("ddd", "tdd");
		assertThat(response.items().get(0).status()).isEqualTo(PostStatus.PUBLISHED);
		assertThat(response.items().get(0).createdAt()).isEqualTo(NOW);
		assertThat(response.items().get(0).updatedAt()).isEqualTo(NOW);
		assertThat(response.items().get(0).publishedAt()).isEqualTo(NOW);
		assertThat(response.page()).isEqualTo(0);
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.totalElements()).isEqualTo(1);
		assertThat(response.totalPages()).isEqualTo(1);
		assertThat(response.hasNext()).isFalse();
	}

	@Test
	void 페이지와_크기가_null이면_기본값으로_조회한다() {
		FakePostListQueryRepository repository = new FakePostListQueryRepository();
		PostListApiService service = new PostListApiService(new PostListQueryService(repository));

		service.getList(null, null);

		assertThat(repository.마지막_조회_조건().page()).isEqualTo(0);
		assertThat(repository.마지막_조회_조건().size()).isEqualTo(20);
	}

	private PostListItem createItem() {
		return new PostListItem(
			new PostId(1L),
			new AuthorId(10L),
			new PostTitle("DDD 시작하기"),
			new PostSummary("DDD 소개"),
			List.of(new TagName("ddd"), new TagName("tdd")),
			PostStatus.PUBLISHED,
			NOW,
			NOW,
			NOW
		);
	}
}
```

- [ ] **Step 2: Run API service test to verify failure**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostListApiServiceTest
```

Expected:

- `BUILD FAILED`
- Compilation errors mention missing response/service classes.

- [ ] **Step 3: Add list API response/service classes**

Create `backend/src/main/java/com/dddblog/backend/blog/api/PostListItemResponse.java`:

```java
package com.dddblog.backend.blog.api;

import java.time.Instant;
import java.util.List;

import com.dddblog.backend.blog.domain.PostStatus;

public record PostListItemResponse(
	Long postId,
	Long authorId,
	String title,
	String summary,
	List<String> tags,
	PostStatus status,
	Instant createdAt,
	Instant updatedAt,
	Instant publishedAt
) {
}
```

Create `backend/src/main/java/com/dddblog/backend/blog/api/PostListResponse.java`:

```java
package com.dddblog.backend.blog.api;

import java.util.List;

public record PostListResponse(
	List<PostListItemResponse> items,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {
}
```

Create `backend/src/main/java/com/dddblog/backend/blog/api/PostListApiService.java`:

```java
package com.dddblog.backend.blog.api;

import org.springframework.stereotype.Service;

import com.dddblog.backend.blog.application.PostListItem;
import com.dddblog.backend.blog.application.PostListPage;
import com.dddblog.backend.blog.application.PostListQuery;
import com.dddblog.backend.blog.application.PostListQueryService;
import com.dddblog.backend.blog.domain.TagName;

@Service
public class PostListApiService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;

	private final PostListQueryService postListQueryService;

	public PostListApiService(PostListQueryService postListQueryService) {
		this.postListQueryService = postListQueryService;
	}

	public PostListResponse getList(Integer page, Integer size) {
		PostListPage postListPage = postListQueryService.getList(new PostListQuery(
			page == null ? DEFAULT_PAGE : page,
			size == null ? DEFAULT_SIZE : size
		));
		return new PostListResponse(
			postListPage.items().stream()
				.map(this::toResponse)
				.toList(),
			postListPage.page(),
			postListPage.size(),
			postListPage.totalElements(),
			postListPage.totalPages(),
			postListPage.hasNext()
		);
	}

	private PostListItemResponse toResponse(PostListItem item) {
		return new PostListItemResponse(
			item.postId().value(),
			item.authorId().value(),
			item.title().value(),
			item.summary().value(),
			item.tags().stream()
				.map(TagName::value)
				.toList(),
			item.status(),
			item.createdAt(),
			item.updatedAt(),
			item.publishedAt()
		);
	}
}
```

- [ ] **Step 4: Run API service test**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostListApiServiceTest
```

Expected:

- `BUILD SUCCESSFUL`

- [ ] **Step 5: Add controller tests for public list**

In `PostControllerTest`, add a mock:

```java
	@MockitoBean
	private PostListApiService postListApiService;
```

Add controller tests:

```java
	@Test
	void 공개_글_목록을_200으로_반환한다() throws Exception {
		Instant now = Instant.parse("2026-06-27T10:15:30Z");
		when(postListApiService.getList(0, 20))
			.thenReturn(new PostListResponse(
				List.of(new PostListItemResponse(
					10L,
					1L,
					"DDD 시작하기",
					"DDD 소개",
					List.of("ddd", "tdd"),
					PostStatus.PUBLISHED,
					now,
					now,
					now
				)),
				0,
				20,
				1,
				1,
				false
			));

		mockMvc.perform(get("/api/posts")
				.param("page", "0")
				.param("size", "20"))
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
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.totalPages").value(1))
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
		when(postListApiService.getList(-1, 20))
			.thenThrow(new IllegalArgumentException("Page must be zero or positive."));

		mockMvc.perform(get("/api/posts")
				.param("page", "-1")
				.param("size", "20"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Page must be zero or positive."));
	}

	@Test
	void 페이지_크기가_유효하지_않으면_400을_반환한다() throws Exception {
		when(postListApiService.getList(0, 51))
			.thenThrow(new IllegalArgumentException("Page size must be 50 or less."));

		mockMvc.perform(get("/api/posts")
				.param("page", "0")
				.param("size", "51"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Page size must be 50 or less."));
	}
```

Add import if needed:

```java
import java.time.Instant;
```

- [ ] **Step 6: Run controller test to verify failure**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostControllerTest
```

Expected:

- `BUILD FAILED` or test failure for missing mapping/unauthorized access.

- [ ] **Step 7: Add endpoint and security permit rule**

Modify `PostController` constructor and fields:

```java
	private final PostApiService postApiService;
	private final PostDetailApiService postDetailApiService;
	private final PostListApiService postListApiService;

	public PostController(
		PostApiService postApiService,
		PostDetailApiService postDetailApiService,
		PostListApiService postListApiService
	) {
		this.postApiService = postApiService;
		this.postDetailApiService = postDetailApiService;
		this.postListApiService = postListApiService;
	}
```

Add imports:

```java
import org.springframework.web.bind.annotation.RequestParam;
```

Add endpoint before `getDetail`:

```java
	@GetMapping
	public PostListResponse getList(
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer size
	) {
		return postListApiService.getList(page, size);
	}
```

In `SecurityConfig`, add the public matcher before `GET /api/posts/{postId}`:

```java
				.requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/posts/{postId}").permitAll()
```

- [ ] **Step 8: Run controller test**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostControllerTest
```

Expected:

- `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit public list API**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/main/java/com/dddblog/backend/blog/api/PostListItemResponse.java `
  backend/src/main/java/com/dddblog/backend/blog/api/PostListResponse.java `
  backend/src/main/java/com/dddblog/backend/blog/api/PostListApiService.java `
  backend/src/main/java/com/dddblog/backend/blog/api/PostController.java `
  backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java `
  backend/src/test/java/com/dddblog/backend/blog/api/PostListApiServiceTest.java `
  backend/src/test/java/com/dddblog/backend/blog/api/PostControllerTest.java
git commit -m "feat: add public post list endpoint"
```

Expected:

- Commit succeeds.

---

### Task 7: Add Vertical Integration Test And Full Verification

**Files:**
- Modify: `backend/src/test/java/com/dddblog/backend/blog/api/PostApiIntegrationTest.java`
- No source changes expected after this task unless the integration test exposes a real bug.

- [ ] **Step 1: Extend post API integration tests**

In `PostApiIntegrationTest`, add assertions for timestamps in the existing creation/detail flows:

```java
		assertThat(jdbcTemplate.queryForObject(
			"select created_at is not null from posts where id = ?",
			Boolean.class,
			postId
		)).isTrue();
		assertThat(jdbcTemplate.queryForObject(
			"select updated_at is not null from posts where id = ?",
			Boolean.class,
			postId
		)).isTrue();
		assertThat(jdbcTemplate.queryForObject(
			"select published_at is not null from posts where id = ?",
			Boolean.class,
			postId
		)).isTrue();
```

In the public detail integration assertion, add:

```java
			.andExpect(jsonPath("$.createdAt").isString())
			.andExpect(jsonPath("$.updatedAt").isString())
			.andExpect(jsonPath("$.publishedAt").isString());
```

Add this new vertical list flow:

```java
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

		createPost(accessToken, "첫 번째 공개 글", "PUBLISHED");
		createPost(accessToken, "임시 글", "DRAFT");
		createPost(accessToken, "숨김 글", "HIDDEN");
		MvcResult secondPublishedResult = createPost(accessToken, "두 번째 공개 글", "PUBLISHED");

		Long secondPublishedPostId = objectMapper.readTree(secondPublishedResult.getResponse().getContentAsString())
			.get("postId")
			.asLong();
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
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.totalPages").value(1))
			.andExpect(jsonPath("$.hasNext").value(false));
	}

	private MvcResult createPost(String accessToken, String title, String status) throws Exception {
		return mockMvc.perform(post("/api/posts")
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
	}
```

- [ ] **Step 2: Run post API integration test**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostApiIntegrationTest
```

Expected:

- `BUILD SUCCESSFUL`
- Docker must be running for MySQL Testcontainers.

- [ ] **Step 3: Commit vertical integration test**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/test/java/com/dddblog/backend/blog/api/PostApiIntegrationTest.java
git commit -m "test: verify public post list flow"
```

Expected:

- Commit succeeds.

- [ ] **Step 4: Run full backend tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --rerun-tasks
```

Expected:

- `BUILD SUCCESSFUL`
- Hibernate schema-drop noise can appear during shutdown, but the Gradle result must be successful.

- [ ] **Step 5: Check Korean test naming rule**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected:

- No output.

- [ ] **Step 6: Check pure domain/application packages for Spring/JPA annotations**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional|@Configuration|@Bean'
```

Expected:

- No output.

- [ ] **Step 7: Check H2 was not reintroduced**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
rg -n "h2database|jdbc:h2|H2Dialect|com\.h2database" .
```

Expected:

- No matches. `rg` exits with code `1` when there are no matches.

- [ ] **Step 8: Check whitespace and git status**

Run:

```powershell
cd C:\dev\study\ddd-blog
git diff --check
git status --short
```

Expected:

- `git diff --check` prints no output.
- `git status --short` has no uncommitted implementation changes.

---

## Self-Review

### Spec Coverage

- Post timestamp model: Tasks 1-3.
- Clock-based creation time: Task 2.
- Public detail response timestamp fields: Task 3.
- Public `GET /api/posts`: Task 6.
- `PUBLISHED` only: Task 5 JPA adapter and Task 7 integration flow.
- Pagination defaults and validation: Tasks 4 and 6.
- `publishedAt DESC`, `postId DESC`: Task 5.
- Read-only list query port separate from `PostRepository`: Tasks 4 and 5.
- Security `permitAll`: Task 6.
- Full verification commands: Task 7.

### Placeholder Scan

The plan contains no unfinished markers or undefined task placeholders. Each code-changing task includes exact paths, concrete snippets or complete file content, run commands, and expected outcomes.

### Type Consistency

- Application list types use `PostListQuery`, `PostListItem`, `PostListPage`, `PostListQueryRepository`, and `PostListQueryService` consistently.
- API list types use `PostListItemResponse`, `PostListResponse`, and `PostListApiService` consistently.
- Timestamp fields are consistently named `createdAt`, `updatedAt`, and `publishedAt`.
- Sorting uses JPA property names `publishedAt` and `id`, matching `JpaPostEntity`.
