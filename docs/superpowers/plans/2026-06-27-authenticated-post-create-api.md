# Authenticated Post Creation API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 인증된 회원이 `POST /api/posts`로 `MARKDOWN` 본문 글을 작성하고, 작성자 ID가 JWT 인증 주체에서 저장되도록 구현한다.

**Architecture:** Blog 도메인에 `PostContentType`을 추가하고 `Post`가 본문 형식을 보관하게 한다. API 계층은 `PostApiService`에서 인증 member ID와 요청 본문을 `CreatePostCommand`로 변환하며, 현재 HTTP API에서는 `MARKDOWN`만 허용한다. 순수 도메인/애플리케이션 계층은 Spring/JPA annotation 없이 유지하고, Spring wiring은 `blog.config`에서 담당한다.

**Tech Stack:** Java 21, Spring Boot 3.5.0, Spring MVC, Spring Security, Spring Data JPA, MySQL Testcontainers, JUnit 5, AssertJ, Mockito, MockMvc

---

## File Structure

- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/PostContentType.java`
  - 글 본문 저장 형식을 표현하는 도메인 enum.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/domain/Post.java`
  - `PostContentType` 필드, null 검증, accessor 추가.
- Modify: `backend/src/test/java/com/dddblog/backend/blog/domain/PostTest.java`
  - `Post` 생성자 호출에 `PostContentType.MARKDOWN` 추가, 본문 형식 보관/검증 테스트 추가.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostCommand.java`
  - `PostContentType contentType` 필드 추가.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostService.java`
  - command의 content type을 `Post` 생성자로 전달.
- Modify: `backend/src/test/java/com/dddblog/backend/blog/application/CreatePostServiceTest.java`
  - command 생성부에 `PostContentType.MARKDOWN` 추가, 저장된 글의 본문 형식 검증 추가.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostEntity.java`
  - `content_type` 컬럼 매핑 추가.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapter.java`
  - `post.contentType()`을 entity에 전달.
- Modify: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`
  - persisted `content_type` 검증 추가.
- Create: `backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java`
  - 순수 `CreatePostService`를 Spring Bean으로 노출.
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostRequest.java`
  - `POST /api/posts` 요청 모델.
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostResponse.java`
  - `POST /api/posts` 응답 모델.
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostApiService.java`
  - API 요청을 application command로 변환하고 현재 허용 본문 형식을 검증.
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostController.java`
  - 인증된 글 작성 HTTP endpoint.
- Create: `backend/src/test/java/com/dddblog/backend/blog/api/PostApiServiceTest.java`
  - API service 변환과 `MARKDOWN` 제한 검증.
- Create: `backend/src/test/java/com/dddblog/backend/blog/api/PostControllerTest.java`
  - WebMvc controller 보안/응답 검증.
- Create: `backend/src/test/java/com/dddblog/backend/blog/api/PostApiIntegrationTest.java`
  - 회원가입, 로그인, 글 작성, DB 저장값 검증.

---

### Task 1: Add Post Content Type To Domain And Application

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/PostContentType.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/domain/Post.java`
- Modify: `backend/src/test/java/com/dddblog/backend/blog/domain/PostTest.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostCommand.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostService.java`
- Modify: `backend/src/test/java/com/dddblog/backend/blog/application/CreatePostServiceTest.java`
- Modify: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`

- [ ] **Step 1: Write the failing domain and application tests**

In `backend/src/test/java/com/dddblog/backend/blog/domain/PostTest.java`, update the imports and every `new Post(...)` call so `PostContentType.MARKDOWN` is passed between `PostContent` and `PostSummary`.

Example replacement:

```java
Post post = new Post(
	new AuthorId(1L),
	new PostTitle("DDD 시작하기"),
	new PostContent("본문"),
	PostContentType.MARKDOWN,
	new PostSummary("요약"),
	List.of(),
	PostStatus.DRAFT
);
```

Add these tests to `PostTest`:

```java
@Test
void 글을_생성하면_본문_형식을_가진다() {
	Post post = new Post(
		new AuthorId(1L),
		new PostTitle("DDD 시작하기"),
		new PostContent("본문"),
		PostContentType.MARKDOWN,
		new PostSummary("요약"),
		List.of(),
		PostStatus.DRAFT
	);

	assertThat(post.contentType()).isEqualTo(PostContentType.MARKDOWN);
}

@Test
void 본문_형식이_null이면_글을_생성할_수_없다() {
	assertThatThrownBy(() -> new Post(
		new AuthorId(1L),
		new PostTitle("DDD 시작하기"),
		new PostContent("본문"),
		null,
		new PostSummary("요약"),
		List.of(),
		PostStatus.DRAFT
	))
		.isInstanceOf(IllegalArgumentException.class)
		.hasMessage("Post content type must not be null.");
}
```

In the existing `글을_생성한다` test, add this assertion:

```java
assertThat(post.contentType()).isEqualTo(PostContentType.MARKDOWN);
```

In `backend/src/test/java/com/dddblog/backend/blog/application/CreatePostServiceTest.java`, add this import:

```java
import com.dddblog.backend.blog.domain.PostContentType;
```

Update every `new CreatePostCommand(...)` call so `PostContentType.MARKDOWN` is passed between `title` and `content`.

Example replacement:

```java
CreatePostCommand command = new CreatePostCommand(
	1L,
	"DDD 시작하기",
	PostContentType.MARKDOWN,
	"# DDD\n\n본문",
	"DDD 소개",
	List.of("ddd", "tdd"),
	PostStatus.DRAFT
);
```

In the existing `저장된_글은_요청_값을_도메인_값으로_가진다` test, add this assertion:

```java
assertThat(savedPost.contentType()).isEqualTo(PostContentType.MARKDOWN);
```

In `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`, add this import:

```java
import com.dddblog.backend.blog.domain.PostContentType;
```

Update every `new Post(...)` call so `PostContentType.MARKDOWN` is passed between `PostContent` and `PostSummary`.

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostTest --tests com.dddblog.backend.blog.application.CreatePostServiceTest
```

Expected:

- FAIL
- Compilation errors mention missing `PostContentType`, missing `CreatePostCommand` constructor, or missing `Post` constructor.

- [ ] **Step 3: Create `PostContentType`**

Create `backend/src/main/java/com/dddblog/backend/blog/domain/PostContentType.java`:

```java
package com.dddblog.backend.blog.domain;

public enum PostContentType {
	MARKDOWN,
	HTML
}
```

- [ ] **Step 4: Replace `Post` with the content type implementation**

Replace `backend/src/main/java/com/dddblog/backend/blog/domain/Post.java` with:

```java
package com.dddblog.backend.blog.domain;

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

	public Post(
		AuthorId authorId,
		PostTitle title,
		PostContent content,
		PostContentType contentType,
		PostSummary summary,
		List<TagName> tags,
		PostStatus status
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
}
```

- [ ] **Step 5: Replace `CreatePostCommand` with the content type field**

Replace `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostCommand.java` with:

```java
package com.dddblog.backend.blog.application;

import java.util.List;

import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostStatus;

public record CreatePostCommand(
	Long authorId,
	String title,
	PostContentType contentType,
	String content,
	String summary,
	List<String> tags,
	PostStatus status
) {
}
```

- [ ] **Step 6: Replace `CreatePostService` so it passes content type to `Post`**

Replace `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostService.java` with:

```java
package com.dddblog.backend.blog.application;

import java.util.List;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

public class CreatePostService {

	private final PostRepository postRepository;

	public CreatePostService(PostRepository postRepository) {
		this.postRepository = postRepository;
	}

	public PostId create(CreatePostCommand command) {
		if (command == null) {
			throw new IllegalArgumentException("Create post command must not be null.");
		}
		Post post = new Post(
			new AuthorId(command.authorId()),
			new PostTitle(command.title()),
			new PostContent(command.content()),
			command.contentType(),
			new PostSummary(command.summary()),
			toTagNames(command.tags()),
			command.status()
		);
		return postRepository.save(post);
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

- [ ] **Step 7: Run domain and application tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostTest --tests com.dddblog.backend.blog.application.CreatePostServiceTest
```

Expected:

- `BUILD SUCCESSFUL`
- `PostTest` and `CreatePostServiceTest` pass.

- [ ] **Step 8: Commit domain and application content type changes**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api
git add backend/src/main/java/com/dddblog/backend/blog/domain/PostContentType.java `
  backend/src/main/java/com/dddblog/backend/blog/domain/Post.java `
  backend/src/test/java/com/dddblog/backend/blog/domain/PostTest.java `
  backend/src/main/java/com/dddblog/backend/blog/application/CreatePostCommand.java `
  backend/src/main/java/com/dddblog/backend/blog/application/CreatePostService.java `
  backend/src/test/java/com/dddblog/backend/blog/application/CreatePostServiceTest.java `
  backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java
git commit -m "feat: add post content type"
```

Expected:

- Commit succeeds.

---

### Task 2: Persist Post Content Type

**Files:**
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostEntity.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapter.java`
- Modify: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`

- [ ] **Step 1: Write the failing persistence assertion**

In `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`, in `글을_저장하면_본문_값이_posts에_저장된다`, add:

```java
assertThat(savedPost.contentType()).isEqualTo(PostContentType.MARKDOWN);
```

The test method should verify these fields:

```java
assertThat(savedPost.authorId()).isEqualTo(1L);
assertThat(savedPost.title()).isEqualTo("DDD 시작하기");
assertThat(savedPost.contentType()).isEqualTo(PostContentType.MARKDOWN);
assertThat(savedPost.contentMarkdown()).isEqualTo("# DDD\n\n본문");
assertThat(savedPost.summary()).isEqualTo("DDD 소개");
assertThat(savedPost.status()).isEqualTo(PostStatus.DRAFT);
assertThat(savedPost.tags()).extracting(JpaTagEntity::name).containsExactlyInAnyOrder("ddd", "tdd");
```

- [ ] **Step 2: Run persistence test to verify it fails**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.persistence.JpaPostRepositoryAdapterTest
```

Expected:

- FAIL
- Compilation error mentions `contentType()` is missing on `JpaPostEntity`, or the persisted value is missing before implementation.

- [ ] **Step 3: Replace `JpaPostEntity` with content type mapping**

Replace `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostEntity.java` with:

```java
package com.dddblog.backend.blog.persistence;

import java.util.LinkedHashSet;
import java.util.Set;

import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "posts")
class JpaPostEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "author_id", nullable = false)
	private Long authorId;

	@Column(nullable = false, length = 100)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(name = "content_type", nullable = false)
	private PostContentType contentType;

	@Column(name = "content_markdown", nullable = false, columnDefinition = "text")
	private String contentMarkdown;

	@Column(nullable = false, length = 300)
	private String summary;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PostStatus status;

	@ManyToMany
	@JoinTable(
		name = "post_tags",
		joinColumns = @JoinColumn(name = "post_id"),
		inverseJoinColumns = @JoinColumn(name = "tag_id"),
		uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "tag_id"})
	)
	private Set<JpaTagEntity> tags = new LinkedHashSet<>();

	protected JpaPostEntity() {
	}

	JpaPostEntity(
		Long authorId,
		String title,
		PostContentType contentType,
		String contentMarkdown,
		String summary,
		PostStatus status,
		Set<JpaTagEntity> tags
	) {
		this.authorId = authorId;
		this.title = title;
		this.contentType = contentType;
		this.contentMarkdown = contentMarkdown;
		this.summary = summary;
		this.status = status;
		this.tags = new LinkedHashSet<>(tags);
	}

	Long id() {
		return id;
	}

	Long authorId() {
		return authorId;
	}

	String title() {
		return title;
	}

	PostContentType contentType() {
		return contentType;
	}

	String contentMarkdown() {
		return contentMarkdown;
	}

	String summary() {
		return summary;
	}

	PostStatus status() {
		return status;
	}

	Set<JpaTagEntity> tags() {
		return Set.copyOf(tags);
	}
}
```

- [ ] **Step 4: Update `JpaPostRepositoryAdapter` constructor call**

In `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapter.java`, replace the entity creation block with:

```java
JpaPostEntity entity = new JpaPostEntity(
	post.authorId().value(),
	post.title().value(),
	post.contentType(),
	post.content().value(),
	post.summary().value(),
	post.status(),
	tags
);
```

- [ ] **Step 5: Run persistence tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.persistence.JpaPostRepositoryAdapterTest
```

Expected:

- `BUILD SUCCESSFUL`
- Hibernate creates `posts.content_type` in the Testcontainers schema.

- [ ] **Step 6: Commit persistence content type changes**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api
git add backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostEntity.java `
  backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapter.java `
  backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java
git commit -m "feat: persist post content type"
```

Expected:

- Commit succeeds.

---

### Task 3: Add Blog Application Spring Wiring

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java`

- [ ] **Step 1: Write the configuration**

Create `backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java`:

```java
package com.dddblog.backend.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dddblog.backend.blog.application.CreatePostService;
import com.dddblog.backend.blog.application.PostRepository;

@Configuration
public class BlogApplicationConfig {

	@Bean
	CreatePostService createPostService(PostRepository postRepository) {
		return new CreatePostService(postRepository);
	}
}
```

- [ ] **Step 2: Run compile test for configuration**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat compileJava
```

Expected:

- `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit configuration**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api
git add backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java
git commit -m "feat: configure blog application service"
```

Expected:

- Commit succeeds.

---

### Task 4: Add Post API Service

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostRequest.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostResponse.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostApiService.java`
- Create: `backend/src/test/java/com/dddblog/backend/blog/api/PostApiServiceTest.java`

- [ ] **Step 1: Write the failing API service test**

Create `backend/src/test/java/com/dddblog/backend/blog/api/PostApiServiceTest.java`:

```java
package com.dddblog.backend.blog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.blog.application.CreatePostService;
import com.dddblog.backend.blog.application.FakePostRepository;
import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.member.domain.MemberId;

class PostApiServiceTest {

	@Test
	void 글_작성_요청을_애플리케이션_서비스에_전달한다() {
		FakePostRepository postRepository = new FakePostRepository();
		PostApiService postApiService = new PostApiService(new CreatePostService(postRepository));
		PostRequest request = new PostRequest(
			"DDD 시작하기",
			PostContentType.MARKDOWN,
			"# DDD\n\n본문",
			"DDD 소개",
			List.of("DDD", "TDD"),
			PostStatus.PUBLISHED
		);

		PostResponse response = postApiService.create(new MemberId(1L), request);

		assertThat(response.postId()).isEqualTo(1L);
		Post savedPost = postRepository.savedPosts().get(0);
		assertThat(savedPost.authorId().value()).isEqualTo(1L);
		assertThat(savedPost.title().value()).isEqualTo("DDD 시작하기");
		assertThat(savedPost.contentType()).isEqualTo(PostContentType.MARKDOWN);
		assertThat(savedPost.content().value()).isEqualTo("# DDD\n\n본문");
		assertThat(savedPost.summary().value()).isEqualTo("DDD 소개");
		assertThat(savedPost.tags()).extracting(tag -> tag.value()).containsExactly("ddd", "tdd");
		assertThat(savedPost.status()).isEqualTo(PostStatus.PUBLISHED);
	}

	@Test
	void HTML_본문_형식이면_글을_생성할_수_없다() {
		FakePostRepository postRepository = new FakePostRepository();
		PostApiService postApiService = new PostApiService(new CreatePostService(postRepository));
		PostRequest request = new PostRequest(
			"DDD 시작하기",
			PostContentType.HTML,
			"<p>본문</p>",
			"DDD 소개",
			List.of(),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> postApiService.create(new MemberId(1L), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content type must be MARKDOWN.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}

	@Test
	void 본문_형식이_null이면_글을_생성할_수_없다() {
		FakePostRepository postRepository = new FakePostRepository();
		PostApiService postApiService = new PostApiService(new CreatePostService(postRepository));
		PostRequest request = new PostRequest(
			"DDD 시작하기",
			null,
			"본문",
			"DDD 소개",
			List.of(),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> postApiService.create(new MemberId(1L), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content type must be MARKDOWN.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}
}
```

- [ ] **Step 2: Make `FakePostRepository` visible to the API service test**

In `backend/src/test/java/com/dddblog/backend/blog/application/FakePostRepository.java`, change the class and `savedPosts()` visibility:

```java
public class FakePostRepository implements PostRepository {
```

```java
public List<Post> savedPosts() {
	return List.copyOf(savedPosts);
}
```

- [ ] **Step 3: Run API service test to verify it fails**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostApiServiceTest
```

Expected:

- FAIL
- Compilation errors mention missing `PostRequest`, `PostResponse`, or `PostApiService`.

- [ ] **Step 4: Create `PostRequest`**

Create `backend/src/main/java/com/dddblog/backend/blog/api/PostRequest.java`:

```java
package com.dddblog.backend.blog.api;

import java.util.List;

import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostStatus;

public record PostRequest(
	String title,
	PostContentType contentType,
	String content,
	String summary,
	List<String> tags,
	PostStatus status
) {
}
```

- [ ] **Step 5: Create `PostResponse`**

Create `backend/src/main/java/com/dddblog/backend/blog/api/PostResponse.java`:

```java
package com.dddblog.backend.blog.api;

public record PostResponse(Long postId) {
}
```

- [ ] **Step 6: Create `PostApiService`**

Create `backend/src/main/java/com/dddblog/backend/blog/api/PostApiService.java`:

```java
package com.dddblog.backend.blog.api;

import org.springframework.stereotype.Service;

import com.dddblog.backend.blog.application.CreatePostCommand;
import com.dddblog.backend.blog.application.CreatePostService;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.member.domain.MemberId;

@Service
public class PostApiService {

	private final CreatePostService createPostService;

	public PostApiService(CreatePostService createPostService) {
		this.createPostService = createPostService;
	}

	public PostResponse create(MemberId memberId, PostRequest request) {
		validateMarkdownContentType(request.contentType());
		CreatePostCommand command = new CreatePostCommand(
			memberId.value(),
			request.title(),
			request.contentType(),
			request.content(),
			request.summary(),
			request.tags(),
			request.status()
		);
		PostId postId = createPostService.create(command);
		return new PostResponse(postId.value());
	}

	private void validateMarkdownContentType(PostContentType contentType) {
		if (contentType != PostContentType.MARKDOWN) {
			throw new IllegalArgumentException("Post content type must be MARKDOWN.");
		}
	}
}
```

- [ ] **Step 7: Run API service test**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostApiServiceTest
```

Expected:

- `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit API service**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api
git add backend/src/main/java/com/dddblog/backend/blog/api/PostRequest.java `
  backend/src/main/java/com/dddblog/backend/blog/api/PostResponse.java `
  backend/src/main/java/com/dddblog/backend/blog/api/PostApiService.java `
  backend/src/test/java/com/dddblog/backend/blog/api/PostApiServiceTest.java `
  backend/src/test/java/com/dddblog/backend/blog/application/FakePostRepository.java
git commit -m "feat: add post api service"
```

Expected:

- Commit succeeds.

---

### Task 5: Add Post Controller

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostController.java`
- Create: `backend/src/test/java/com/dddblog/backend/blog/api/PostControllerTest.java`

- [ ] **Step 1: Write the failing controller test**

Create `backend/src/test/java/com/dddblog/backend/blog/api/PostControllerTest.java`:

```java
package com.dddblog.backend.blog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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

@WebMvcTest(PostController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class PostControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PostApiService postApiService;

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
	void 토큰이_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/posts")
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
```

- [ ] **Step 2: Run controller test to verify it fails**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostControllerTest
```

Expected:

- FAIL
- Compilation error mentions missing `PostController`.

- [ ] **Step 3: Create `PostController`**

Create `backend/src/main/java/com/dddblog/backend/blog/api/PostController.java`:

```java
package com.dddblog.backend.blog.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dddblog.backend.auth.application.AuthenticationFailedException;
import com.dddblog.backend.auth.security.AuthenticatedMember;

@RestController
@RequestMapping("/api/posts")
public class PostController {

	private final PostApiService postApiService;

	public PostController(PostApiService postApiService) {
		this.postApiService = postApiService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PostResponse create(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody PostRequest request
	) {
		if (authenticatedMember == null) {
			throw new AuthenticationFailedException();
		}
		return postApiService.create(authenticatedMember.memberId(), request);
	}
}
```

- [ ] **Step 4: Run controller test**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostControllerTest
```

Expected:

- `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit controller**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api
git add backend/src/main/java/com/dddblog/backend/blog/api/PostController.java `
  backend/src/test/java/com/dddblog/backend/blog/api/PostControllerTest.java
git commit -m "feat: add post creation controller"
```

Expected:

- Commit succeeds.

---

### Task 6: Add Authenticated Post Creation Integration Test

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/blog/api/PostApiIntegrationTest.java`

- [ ] **Step 1: Write the failing integration test**

Create `backend/src/test/java/com/dddblog/backend/blog/api/PostApiIntegrationTest.java`:

```java
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
```

- [ ] **Step 2: Run integration test**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostApiIntegrationTest
```

Expected:

- `BUILD SUCCESSFUL`
- Docker must be running for MySQL Testcontainers.

- [ ] **Step 3: Commit integration test**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api
git add backend/src/test/java/com/dddblog/backend/blog/api/PostApiIntegrationTest.java
git commit -m "test: verify authenticated post creation flow"
```

Expected:

- Commit succeeds.

---

### Task 7: Full Verification

**Files:**
- No source changes expected.

- [ ] **Step 1: Run full backend tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --rerun-tasks
```

Expected:

- `BUILD SUCCESSFUL`
- Hibernate schema-drop noise can appear during shutdown, but Gradle result must be successful.

- [ ] **Step 2: Check Korean test naming rule**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected:

- No output.

- [ ] **Step 3: Check pure domain/application packages for Spring/JPA annotations**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional|@Configuration|@Bean'
```

Expected:

- No output.

- [ ] **Step 4: Check H2 was not reintroduced**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
rg -n "h2database|jdbc:h2|H2Dialect|com\.h2database" .
```

Expected:

- No matches. `rg` exits with code `1` when there are no matches.

- [ ] **Step 5: Check whitespace**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api
git diff --check
```

Expected:

- No output.

- [ ] **Step 6: Check git status and recent commits**

Run:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api
git status --short
git log --oneline --max-count=8
```

Expected:

- `git status --short` has no output.
- Recent commits include:
  - `test: verify authenticated post creation flow`
  - `feat: add post creation controller`
  - `feat: add post api service`
  - `feat: configure blog application service`
  - `feat: persist post content type`
  - `feat: add post content type`
  - `docs: add authenticated post creation api design`

---

## Self-Review

### Spec Coverage

- `POST /api/posts` endpoint: Task 5.
- Authenticated member as author ID: Task 5 controller test and Task 6 integration test.
- `contentType` and `content` API model: Task 4.
- `MARKDOWN` only in current API: Task 4 service tests and Task 5 controller error test.
- `PostContentType` domain concept: Task 1.
- `posts.content_type` persistence: Task 2 and Task 6.
- `BlogApplicationConfig` wiring: Task 3.
- No post read/update/delete, HTML sanitize, cover image, FK, migration, or frontend work: no tasks add those.
- Full verification commands: Task 7.

### Placeholder Scan

- The plan contains no unfinished marker text, incomplete file paths, or vague validation steps.
- Code-changing steps include exact class, method, or assertion content.
- Verification steps include commands and expected output.

### Type Consistency

- `PostContentType` is defined in `com.dddblog.backend.blog.domain`.
- `Post` constructor parameter order is consistently `authorId`, `title`, `content`, `contentType`, `summary`, `tags`, `status`.
- `CreatePostCommand` parameter order is consistently `authorId`, `title`, `contentType`, `content`, `summary`, `tags`, `status`.
- `PostRequest` parameter order matches JSON field order and API service mapping.
- `PostApiService.create(MemberId, PostRequest)` returns `PostResponse`.
