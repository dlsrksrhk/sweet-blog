# Post Detail Read API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 비회원이 `GET /api/posts/{postId}`로 `PUBLISHED` 글 상세를 조회하고, 없는 글과 비공개 글은 `404 Post not found.`로 받게 한다.

**Architecture:** 글 작성용 `PostRepository`는 write port로 유지하고, 상세 조회는 `PostDetailQueryService`와 `PostDetailQueryRepository` read path로 분리한다. JPA read adapter는 `PUBLISHED` 상태만 조회해 `PostDetail` read model로 변환하고, API 계층은 `PostDetailResponse`로 변환한다. `GET /api/posts/{postId}`만 public으로 열고 `POST /api/posts` 인증 정책은 유지한다.

**Tech Stack:** Java 21, Spring Boot 3.5.0, Spring MVC, Spring Security, Spring Data JPA, MySQL Testcontainers, JUnit 5, AssertJ, Mockito, MockMvc

---

## File Structure

- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostDetail.java`
  - 공개 글 상세 조회 read model.
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostDetailQueryRepository.java`
  - 공개 글 상세 조회용 application port.
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostDetailQueryService.java`
  - 공개 글 상세 조회 use case.
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostNotFoundException.java`
  - 공개 조회에서 없는 글과 비공개 글을 같은 404로 표현하는 예외.
- Create: `backend/src/test/java/com/dddblog/backend/blog/application/FakePostDetailQueryRepository.java`
  - application/API service 테스트용 fake query repository.
- Create: `backend/src/test/java/com/dddblog/backend/blog/application/PostDetailQueryServiceTest.java`
  - Spring Context 없이 query service 동작 검증.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java`
  - 순수 `PostDetailQueryService`를 Spring Bean으로 노출.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaPostRepository.java`
  - `findByIdAndStatus(Long id, PostStatus status)` 추가.
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostDetailQueryRepositoryAdapter.java`
  - JPA entity를 `PostDetail`로 변환하는 read adapter.
- Create: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostDetailQueryRepositoryAdapterTest.java`
  - MySQL Testcontainers 기반 read adapter 검증.
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostDetailResponse.java`
  - 상세 조회 HTTP 응답 모델.
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostDetailApiService.java`
  - path variable을 application query로 변환하고 response를 만든다.
- Create: `backend/src/test/java/com/dddblog/backend/blog/api/PostDetailApiServiceTest.java`
  - 상세 조회 API service 변환 검증.
- Modify: `backend/src/main/java/com/dddblog/backend/blog/api/PostController.java`
  - `GET /api/posts/{postId}` endpoint 추가.
- Modify: `backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java`
  - `PostNotFoundException`을 404로 매핑.
- Modify: `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`
  - `GET /api/posts/{postId}`를 public으로 허용.
- Modify: `backend/src/test/java/com/dddblog/backend/blog/api/PostControllerTest.java`
  - 상세 조회 controller, 404, 400, public access 검증.
- Modify: `backend/src/test/java/com/dddblog/backend/blog/api/PostApiIntegrationTest.java`
  - 회원가입, 로그인, 공개 글 작성, 토큰 없는 상세 조회 세로 흐름 검증.

---

### Task 1: Add Application Query Model And Service

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/blog/application/FakePostDetailQueryRepository.java`
- Create: `backend/src/test/java/com/dddblog/backend/blog/application/PostDetailQueryServiceTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostDetail.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostDetailQueryRepository.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostDetailQueryService.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostNotFoundException.java`

- [ ] **Step 1: Write the failing fake repository**

Create `backend/src/test/java/com/dddblog/backend/blog/application/FakePostDetailQueryRepository.java`:

```java
package com.dddblog.backend.blog.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.dddblog.backend.blog.domain.PostId;

public class FakePostDetailQueryRepository implements PostDetailQueryRepository {

	private final Map<PostId, PostDetail> postDetails = new HashMap<>();

	@Override
	public Optional<PostDetail> findPublishedById(PostId postId) {
		return Optional.ofNullable(postDetails.get(postId));
	}

	public void save(PostDetail postDetail) {
		postDetails.put(postDetail.postId(), postDetail);
	}
}
```

- [ ] **Step 2: Write the failing query service test**

Create `backend/src/test/java/com/dddblog/backend/blog/application/PostDetailQueryServiceTest.java`:

```java
package com.dddblog.backend.blog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

class PostDetailQueryServiceTest {

	@Test
	void 공개된_글_상세를_조회한다() {
		FakePostDetailQueryRepository postDetailQueryRepository = new FakePostDetailQueryRepository();
		PostDetailQueryService postDetailQueryService = new PostDetailQueryService(postDetailQueryRepository);
		PostDetail postDetail = createPostDetail(new PostId(1L));
		postDetailQueryRepository.save(postDetail);

		PostDetail foundPostDetail = postDetailQueryService.getDetail(new PostId(1L));

		assertThat(foundPostDetail).isEqualTo(postDetail);
	}

	@Test
	void 공개된_글이_없으면_조회할_수_없다() {
		FakePostDetailQueryRepository postDetailQueryRepository = new FakePostDetailQueryRepository();
		PostDetailQueryService postDetailQueryService = new PostDetailQueryService(postDetailQueryRepository);

		assertThatThrownBy(() -> postDetailQueryService.getDetail(new PostId(1L)))
			.isInstanceOf(PostNotFoundException.class)
			.hasMessage("Post not found.");
	}

	@Test
	void 조회할_글_ID가_null이면_조회할_수_없다() {
		FakePostDetailQueryRepository postDetailQueryRepository = new FakePostDetailQueryRepository();
		PostDetailQueryService postDetailQueryService = new PostDetailQueryService(postDetailQueryRepository);

		assertThatThrownBy(() -> postDetailQueryService.getDetail(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post id must not be null.");
	}

	private PostDetail createPostDetail(PostId postId) {
		return new PostDetail(
			postId,
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			PostContentType.MARKDOWN,
			new PostContent("# DDD\n\n본문"),
			new PostSummary("DDD 소개"),
			List.of(new TagName("ddd"), new TagName("tdd")),
			PostStatus.PUBLISHED
		);
	}
}
```

- [ ] **Step 3: Run application query service test to verify it fails**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.application.PostDetailQueryServiceTest
```

Expected:

- `BUILD FAILED`
- Compilation errors mention missing `PostDetail`, `PostDetailQueryRepository`, `PostDetailQueryService`, or `PostNotFoundException`.

- [ ] **Step 4: Create `PostDetail`**

Create `backend/src/main/java/com/dddblog/backend/blog/application/PostDetail.java`:

```java
package com.dddblog.backend.blog.application;

import java.util.List;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

public record PostDetail(
	PostId postId,
	AuthorId authorId,
	PostTitle title,
	PostContentType contentType,
	PostContent content,
	PostSummary summary,
	List<TagName> tags,
	PostStatus status
) {

	public PostDetail {
		if (postId == null) {
			throw new IllegalArgumentException("Post id must not be null.");
		}
		if (authorId == null) {
			throw new IllegalArgumentException("Post author must not be null.");
		}
		if (title == null) {
			throw new IllegalArgumentException("Post title must not be null.");
		}
		if (contentType == null) {
			throw new IllegalArgumentException("Post content type must not be null.");
		}
		if (content == null) {
			throw new IllegalArgumentException("Post content must not be null.");
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
		tags = List.copyOf(tags);
	}
}
```

- [ ] **Step 5: Create `PostDetailQueryRepository`**

Create `backend/src/main/java/com/dddblog/backend/blog/application/PostDetailQueryRepository.java`:

```java
package com.dddblog.backend.blog.application;

import java.util.Optional;

import com.dddblog.backend.blog.domain.PostId;

public interface PostDetailQueryRepository {

	Optional<PostDetail> findPublishedById(PostId postId);
}
```

- [ ] **Step 6: Create `PostNotFoundException`**

Create `backend/src/main/java/com/dddblog/backend/blog/application/PostNotFoundException.java`:

```java
package com.dddblog.backend.blog.application;

public class PostNotFoundException extends RuntimeException {

	public PostNotFoundException() {
		super("Post not found.");
	}
}
```

- [ ] **Step 7: Create `PostDetailQueryService`**

Create `backend/src/main/java/com/dddblog/backend/blog/application/PostDetailQueryService.java`:

```java
package com.dddblog.backend.blog.application;

import com.dddblog.backend.blog.domain.PostId;

public class PostDetailQueryService {

	private final PostDetailQueryRepository postDetailQueryRepository;

	public PostDetailQueryService(PostDetailQueryRepository postDetailQueryRepository) {
		this.postDetailQueryRepository = postDetailQueryRepository;
	}

	public PostDetail getDetail(PostId postId) {
		if (postId == null) {
			throw new IllegalArgumentException("Post id must not be null.");
		}
		return postDetailQueryRepository.findPublishedById(postId)
			.orElseThrow(PostNotFoundException::new);
	}
}
```

- [ ] **Step 8: Run application query service test**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.application.PostDetailQueryServiceTest
```

Expected:

- `BUILD SUCCESSFUL`
- `PostDetailQueryServiceTest` passes.

- [ ] **Step 9: Commit application query model and service**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/main/java/com/dddblog/backend/blog/application/PostDetail.java `
  backend/src/main/java/com/dddblog/backend/blog/application/PostDetailQueryRepository.java `
  backend/src/main/java/com/dddblog/backend/blog/application/PostDetailQueryService.java `
  backend/src/main/java/com/dddblog/backend/blog/application/PostNotFoundException.java `
  backend/src/test/java/com/dddblog/backend/blog/application/FakePostDetailQueryRepository.java `
  backend/src/test/java/com/dddblog/backend/blog/application/PostDetailQueryServiceTest.java
git commit -m "feat: add post detail query service"
```

Expected:

- Commit succeeds.

---

### Task 2: Add JPA Post Detail Query Adapter

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostDetailQueryRepositoryAdapterTest.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaPostRepository.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostDetailQueryRepositoryAdapter.java`

- [ ] **Step 1: Write the failing persistence query adapter test**

Create `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostDetailQueryRepositoryAdapterTest.java`:

```java
package com.dddblog.backend.blog.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.dddblog.backend.blog.application.PostDetail;
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
@Import({JpaPostRepositoryAdapter.class, JpaPostDetailQueryRepositoryAdapter.class})
class JpaPostDetailQueryRepositoryAdapterTest extends MysqlDataJpaTestSupport {

	@Autowired
	private JpaPostRepositoryAdapter postRepository;

	@Autowired
	private JpaPostDetailQueryRepositoryAdapter postDetailQueryRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 공개된_글을_ID로_조회한다() {
		PostId postId = postRepository.save(createPost(PostStatus.PUBLISHED));
		entityManager.flush();
		entityManager.clear();

		PostDetail postDetail = postDetailQueryRepository.findPublishedById(postId).orElseThrow();

		assertThat(postDetail.postId()).isEqualTo(postId);
		assertThat(postDetail.authorId()).isEqualTo(new AuthorId(1L));
		assertThat(postDetail.title().value()).isEqualTo("DDD 시작하기");
		assertThat(postDetail.contentType()).isEqualTo(PostContentType.MARKDOWN);
		assertThat(postDetail.content().value()).isEqualTo("# DDD\n\n본문");
		assertThat(postDetail.summary().value()).isEqualTo("DDD 소개");
		assertThat(postDetail.status()).isEqualTo(PostStatus.PUBLISHED);
		assertThat(postDetail.tags()).extracting(TagName::value).containsExactly("ddd", "tdd");
	}

	@Test
	void 임시_저장_글은_조회하지_않는다() {
		PostId postId = postRepository.save(createPost(PostStatus.DRAFT));
		entityManager.flush();
		entityManager.clear();

		assertThat(postDetailQueryRepository.findPublishedById(postId)).isEmpty();
	}

	@Test
	void 숨김_글은_조회하지_않는다() {
		PostId postId = postRepository.save(createPost(PostStatus.HIDDEN));
		entityManager.flush();
		entityManager.clear();

		assertThat(postDetailQueryRepository.findPublishedById(postId)).isEmpty();
	}

	@Test
	void 태그를_이름_오름차순으로_조회한다() {
		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("# DDD\n\n본문"),
			PostContentType.MARKDOWN,
			new PostSummary("DDD 소개"),
			List.of(new TagName("tdd"), new TagName("ddd")),
			PostStatus.PUBLISHED
		);
		PostId postId = postRepository.save(post);
		entityManager.flush();
		entityManager.clear();

		PostDetail postDetail = postDetailQueryRepository.findPublishedById(postId).orElseThrow();

		assertThat(postDetail.tags()).extracting(TagName::value).containsExactly("ddd", "tdd");
	}

	private Post createPost(PostStatus status) {
		return new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("# DDD\n\n본문"),
			PostContentType.MARKDOWN,
			new PostSummary("DDD 소개"),
			List.of(new TagName("ddd"), new TagName("tdd")),
			status
		);
	}
}
```

- [ ] **Step 2: Run persistence query adapter test to verify it fails**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.persistence.JpaPostDetailQueryRepositoryAdapterTest
```

Expected:

- `BUILD FAILED`
- Compilation error mentions missing `JpaPostDetailQueryRepositoryAdapter`.

- [ ] **Step 3: Add Spring Data query method**

Modify `backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaPostRepository.java`:

```java
package com.dddblog.backend.blog.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dddblog.backend.blog.domain.PostStatus;

interface SpringDataJpaPostRepository extends JpaRepository<JpaPostEntity, Long> {

	Optional<JpaPostEntity> findByIdAndStatus(Long id, PostStatus status);
}
```

- [ ] **Step 4: Create JPA query adapter**

Create `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostDetailQueryRepositoryAdapter.java`:

```java
package com.dddblog.backend.blog.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dddblog.backend.blog.application.PostDetail;
import com.dddblog.backend.blog.application.PostDetailQueryRepository;
import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

@Repository
public class JpaPostDetailQueryRepositoryAdapter implements PostDetailQueryRepository {

	private final SpringDataJpaPostRepository postRepository;

	public JpaPostDetailQueryRepositoryAdapter(SpringDataJpaPostRepository postRepository) {
		this.postRepository = postRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<PostDetail> findPublishedById(PostId postId) {
		return postRepository.findByIdAndStatus(postId.value(), PostStatus.PUBLISHED)
			.map(this::toPostDetail);
	}

	private PostDetail toPostDetail(JpaPostEntity entity) {
		return new PostDetail(
			new PostId(entity.id()),
			new AuthorId(entity.authorId()),
			new PostTitle(entity.title()),
			entity.contentType(),
			new PostContent(entity.contentMarkdown()),
			new PostSummary(entity.summary()),
			toTagNames(entity),
			entity.status()
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

- [ ] **Step 5: Run persistence query adapter test**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.persistence.JpaPostDetailQueryRepositoryAdapterTest
```

Expected:

- `BUILD SUCCESSFUL`
- All four `JpaPostDetailQueryRepositoryAdapterTest` scenarios pass.

- [ ] **Step 6: Commit JPA query adapter**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaPostRepository.java `
  backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostDetailQueryRepositoryAdapter.java `
  backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostDetailQueryRepositoryAdapterTest.java
git commit -m "feat: add post detail query adapter"
```

Expected:

- Commit succeeds.

---

### Task 3: Add Post Detail API Service And Spring Wiring

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostDetailResponse.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/api/PostDetailApiService.java`
- Create: `backend/src/test/java/com/dddblog/backend/blog/api/PostDetailApiServiceTest.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java`

- [ ] **Step 1: Write the failing API service test**

Create `backend/src/test/java/com/dddblog/backend/blog/api/PostDetailApiServiceTest.java`:

```java
package com.dddblog.backend.blog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.blog.application.FakePostDetailQueryRepository;
import com.dddblog.backend.blog.application.PostDetail;
import com.dddblog.backend.blog.application.PostDetailQueryService;
import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

class PostDetailApiServiceTest {

	@Test
	void 공개된_글_상세를_응답으로_변환한다() {
		FakePostDetailQueryRepository postDetailQueryRepository = new FakePostDetailQueryRepository();
		PostDetailApiService postDetailApiService = new PostDetailApiService(
			new PostDetailQueryService(postDetailQueryRepository)
		);
		postDetailQueryRepository.save(createPostDetail());

		PostDetailResponse response = postDetailApiService.getDetail(1L);

		assertThat(response.postId()).isEqualTo(1L);
		assertThat(response.authorId()).isEqualTo(10L);
		assertThat(response.title()).isEqualTo("DDD 시작하기");
		assertThat(response.contentType()).isEqualTo(PostContentType.MARKDOWN);
		assertThat(response.content()).isEqualTo("# DDD\n\n본문");
		assertThat(response.summary()).isEqualTo("DDD 소개");
		assertThat(response.tags()).containsExactly("ddd", "tdd");
		assertThat(response.status()).isEqualTo(PostStatus.PUBLISHED);
	}

	@Test
	void 글_ID가_유효하지_않으면_조회할_수_없다() {
		FakePostDetailQueryRepository postDetailQueryRepository = new FakePostDetailQueryRepository();
		PostDetailApiService postDetailApiService = new PostDetailApiService(
			new PostDetailQueryService(postDetailQueryRepository)
		);

		assertThatThrownBy(() -> postDetailApiService.getDetail(0L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post id must be positive.");
	}

	private PostDetail createPostDetail() {
		return new PostDetail(
			new PostId(1L),
			new AuthorId(10L),
			new PostTitle("DDD 시작하기"),
			PostContentType.MARKDOWN,
			new PostContent("# DDD\n\n본문"),
			new PostSummary("DDD 소개"),
			List.of(new TagName("ddd"), new TagName("tdd")),
			PostStatus.PUBLISHED
		);
	}
}
```

- [ ] **Step 2: Run API service test to verify it fails**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostDetailApiServiceTest
```

Expected:

- `BUILD FAILED`
- Compilation errors mention missing `PostDetailApiService` or `PostDetailResponse`.

- [ ] **Step 3: Create `PostDetailResponse`**

Create `backend/src/main/java/com/dddblog/backend/blog/api/PostDetailResponse.java`:

```java
package com.dddblog.backend.blog.api;

import java.util.List;

import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostStatus;

public record PostDetailResponse(
	Long postId,
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

- [ ] **Step 4: Create `PostDetailApiService`**

Create `backend/src/main/java/com/dddblog/backend/blog/api/PostDetailApiService.java`:

```java
package com.dddblog.backend.blog.api;

import org.springframework.stereotype.Service;

import com.dddblog.backend.blog.application.PostDetail;
import com.dddblog.backend.blog.application.PostDetailQueryService;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.TagName;

@Service
public class PostDetailApiService {

	private final PostDetailQueryService postDetailQueryService;

	public PostDetailApiService(PostDetailQueryService postDetailQueryService) {
		this.postDetailQueryService = postDetailQueryService;
	}

	public PostDetailResponse getDetail(Long postId) {
		PostDetail postDetail = postDetailQueryService.getDetail(new PostId(postId));
		return new PostDetailResponse(
			postDetail.postId().value(),
			postDetail.authorId().value(),
			postDetail.title().value(),
			postDetail.contentType(),
			postDetail.content().value(),
			postDetail.summary().value(),
			postDetail.tags().stream()
				.map(TagName::value)
				.toList(),
			postDetail.status()
		);
	}
}
```

- [ ] **Step 5: Wire query service in `BlogApplicationConfig`**

Modify `backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java`:

```java
package com.dddblog.backend.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dddblog.backend.blog.application.CreatePostService;
import com.dddblog.backend.blog.application.PostDetailQueryRepository;
import com.dddblog.backend.blog.application.PostDetailQueryService;
import com.dddblog.backend.blog.application.PostRepository;

@Configuration
public class BlogApplicationConfig {

	@Bean
	CreatePostService createPostService(PostRepository postRepository) {
		return new CreatePostService(postRepository);
	}

	@Bean
	PostDetailQueryService postDetailQueryService(PostDetailQueryRepository postDetailQueryRepository) {
		return new PostDetailQueryService(postDetailQueryRepository);
	}
}
```

- [ ] **Step 6: Run API service test**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostDetailApiServiceTest
```

Expected:

- `BUILD SUCCESSFUL`
- `PostDetailApiServiceTest` passes.

- [ ] **Step 7: Commit API service and wiring**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/main/java/com/dddblog/backend/blog/api/PostDetailResponse.java `
  backend/src/main/java/com/dddblog/backend/blog/api/PostDetailApiService.java `
  backend/src/test/java/com/dddblog/backend/blog/api/PostDetailApiServiceTest.java `
  backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java
git commit -m "feat: add post detail api service"
```

Expected:

- Commit succeeds.

---

### Task 4: Add Public Post Detail Controller Endpoint

**Files:**
- Modify: `backend/src/test/java/com/dddblog/backend/blog/api/PostControllerTest.java`
- Modify: `backend/src/main/java/com/dddblog/backend/blog/api/PostController.java`
- Modify: `backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java`
- Modify: `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`

- [ ] **Step 1: Update controller test imports**

In `backend/src/test/java/com/dddblog/backend/blog/api/PostControllerTest.java`, add these static imports:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
```

Add these imports:

```java
import java.util.List;

import com.dddblog.backend.blog.application.PostNotFoundException;
import com.dddblog.backend.blog.domain.PostContentType;
import com.dddblog.backend.blog.domain.PostStatus;
```

- [ ] **Step 2: Add `PostDetailApiService` mock bean**

In `PostControllerTest`, add this field below the existing `PostApiService` mock:

```java
	@MockitoBean
	private PostDetailApiService postDetailApiService;
```

- [ ] **Step 3: Add failing controller tests**

Add these tests to `PostControllerTest`:

```java
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
				PostStatus.PUBLISHED
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
			.andExpect(jsonPath("$.status").value("PUBLISHED"));
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
				PostStatus.PUBLISHED
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
```

- [ ] **Step 4: Run controller test to verify it fails**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.api.PostControllerTest
```

Expected:

- `BUILD FAILED` or test failures.
- Failures mention no `GET /api/posts/{postId}` mapping, 401 for public GET, or missing 404 handler.

- [ ] **Step 5: Add GET endpoint to `PostController`**

Modify `backend/src/main/java/com/dddblog/backend/blog/api/PostController.java`:

```java
package com.dddblog.backend.blog.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	private final PostDetailApiService postDetailApiService;

	public PostController(PostApiService postApiService, PostDetailApiService postDetailApiService) {
		this.postApiService = postApiService;
		this.postDetailApiService = postDetailApiService;
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

	@GetMapping("/{postId}")
	public PostDetailResponse getDetail(@PathVariable Long postId) {
		return postDetailApiService.getDetail(postId);
	}
}
```

- [ ] **Step 6: Add 404 exception mapping**

Modify `backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java`:

```java
package com.dddblog.backend.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dddblog.backend.auth.application.AuthenticationFailedException;
import com.dddblog.backend.blog.application.PostNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthenticationFailedException.class)
	ResponseEntity<ErrorResponse> handleAuthenticationFailedException(AuthenticationFailedException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(new ErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(PostNotFoundException.class)
	ResponseEntity<ErrorResponse> handlePostNotFoundException(PostNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(new ErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleIllegalArgumentException(IllegalArgumentException exception) {
		return new ErrorResponse(exception.getMessage());
	}
}
```

- [ ] **Step 7: Permit public GET detail in security config**

Modify `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`:

```java
package com.dddblog.backend.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.dddblog.backend.auth.security.JwtAuthenticationEntryPoint;
import com.dddblog.backend.auth.security.JwtAuthenticationFilter;
import com.dddblog.backend.auth.security.JwtProperties;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		JwtAuthenticationFilter jwtAuthenticationFilter,
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
	) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/posts/{postId}").permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
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
- Existing post creation controller tests still pass.
- New public detail controller tests pass.

- [ ] **Step 9: Commit controller endpoint**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/test/java/com/dddblog/backend/blog/api/PostControllerTest.java `
  backend/src/main/java/com/dddblog/backend/blog/api/PostController.java `
  backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java `
  backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java
git commit -m "feat: add public post detail endpoint"
```

Expected:

- Commit succeeds.

---

### Task 5: Add Public Detail Vertical Integration Test

**Files:**
- Modify: `backend/src/test/java/com/dddblog/backend/blog/api/PostApiIntegrationTest.java`

- [ ] **Step 1: Update integration test import**

In `backend/src/test/java/com/dddblog/backend/blog/api/PostApiIntegrationTest.java`, add this static import:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
```

- [ ] **Step 2: Add failing vertical integration test**

Add this test to `PostApiIntegrationTest`:

```java
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
			.andExpect(jsonPath("$.status").value("PUBLISHED"));
	}
```

- [ ] **Step 3: Run integration test**

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
- Both existing post creation integration flow and new public detail flow pass.

- [ ] **Step 4: Commit integration test**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/test/java/com/dddblog/backend/blog/api/PostApiIntegrationTest.java
git commit -m "test: verify public post detail flow"
```

Expected:

- Commit succeeds.

---

### Task 6: Full Verification

**Files:**
- No source changes expected.

- [ ] **Step 1: Run full backend tests**

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

- [ ] **Step 2: Check Korean test naming rule**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected:

- No output.

- [ ] **Step 3: Check pure domain/application packages for Spring/JPA annotations**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional|@Configuration|@Bean'
```

Expected:

- No output.

- [ ] **Step 4: Check H2 was not reintroduced**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
rg -n "h2database|jdbc:h2|H2Dialect|com\.h2database" .
```

Expected:

- No matches. `rg` exits with code `1` when there are no matches.

- [ ] **Step 5: Check whitespace**

Run:

```powershell
cd C:\dev\study\ddd-blog
git diff --check
```

Expected:

- No output.

- [ ] **Step 6: Check git status and recent commits**

Run:

```powershell
cd C:\dev\study\ddd-blog
git status --short
git log --oneline --max-count=8
```

Expected:

- `git status --short` has no output.
- Recent commits include:
  - `test: verify public post detail flow`
  - `feat: add public post detail endpoint`
  - `feat: add post detail api service`
  - `feat: add post detail query adapter`
  - `feat: add post detail query service`
  - `docs: add post detail read api design`

---

## Self-Review

### Spec Coverage

- `GET /api/posts/{postId}` public endpoint: Task 4.
- `PUBLISHED` only: Task 2 repository method uses `findByIdAndStatus(..., PostStatus.PUBLISHED)`.
- Missing, `DRAFT`, and `HIDDEN` all return 404: Task 1 service exception, Task 2 private status tests, Task 4 controller 404 mapping.
- Response fields `postId`, `authorId`, `title`, `contentType`, `content`, `summary`, `tags`, `status`: Task 3 response and tests, Task 5 integration test.
- `authorId` only, no member join: Task 2 maps from `JpaPostEntity.authorId()`.
- Keep `PostRepository` write-only: Task 2 adds separate `PostDetailQueryRepository` adapter, no changes to `PostRepository`.
- Pure domain/application packages stay annotation-free: Task 6 annotation scan.
- Full verification commands included: Task 6.

### Placeholder Scan

- The plan contains no unfinished markers, incomplete file paths, or vague validation steps.
- Code-changing steps include concrete file paths and code blocks.
- Every test task includes a run command and expected outcome.

### Type Consistency

- `PostDetail` field order is consistently `postId`, `authorId`, `title`, `contentType`, `content`, `summary`, `tags`, `status`.
- `PostDetailQueryRepository.findPublishedById(PostId postId)` is used consistently by the service and JPA adapter.
- `PostDetailApiService.getDetail(Long postId)` is used consistently by controller and tests.
- `PostDetailResponse` JSON field names match the spec response contract.
