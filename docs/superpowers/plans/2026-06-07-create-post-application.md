# Create Post Application Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 글 작성 유스케이스를 순수 애플리케이션 서비스로 구현하고 fake repository를 사용해 TDD로 검증한다.

**Architecture:** 도메인 모델은 `blog.domain`에 유지하고, 글 작성 유스케이스 흐름은 `blog.application`에 둔다. `CreatePostService`는 command를 도메인 값 객체와 `Post` aggregate로 변환한 뒤 `PostRepository` 포트에 저장을 요청하고, 저장 결과인 `PostId`를 반환한다. JPA, Spring annotation, Controller, 인증은 이번 단계에서 제외한다.

**Tech Stack:** Java 21, Spring Boot 3.5.0, Gradle Kotlin DSL, JUnit 5, AssertJ

---

## File Structure

- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/PostId.java`
  - 저장된 글의 식별자를 표현하는 값 객체.
- Create: `backend/src/test/java/com/dddblog/backend/blog/domain/PostIdTest.java`
  - `PostId`의 null/0/음수 거부 규칙과 정상 생성을 검증.
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostCommand.java`
  - 글 작성 유스케이스 입력값을 담는 command.
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostRepository.java`
  - 애플리케이션 계층이 의존하는 저장소 포트.
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostService.java`
  - command를 도메인 객체로 변환하고 repository에 저장하는 유스케이스 서비스.
- Create: `backend/src/test/java/com/dddblog/backend/blog/application/FakePostRepository.java`
  - 테스트 전용 in-memory repository.
- Create: `backend/src/test/java/com/dddblog/backend/blog/application/CreatePostServiceTest.java`
  - fake repository로 글 작성 유스케이스를 검증.

---

### Task 1: `PostId` 값 객체 TDD

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/blog/domain/PostIdTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/PostId.java`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `backend/src/test/java/com/dddblog/backend/blog/domain/PostIdTest.java`:

```java
package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostIdTest {

	@Test
	void 글_ID를_생성한다() {
		PostId postId = new PostId(1L);

		assertThat(postId.value()).isEqualTo(1L);
	}

	@Test
	void 글_ID가_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostId(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post id must be positive.");
	}

	@Test
	void 글_ID가_0이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostId(0L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post id must be positive.");
	}

	@Test
	void 글_ID가_음수이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostId(-1L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post id must be positive.");
	}
}
```

- [ ] **Step 2: 실패 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostIdTest
```

Expected:

- FAIL
- Compilation error mentioning `cannot find symbol class PostId`.

- [ ] **Step 3: 최소 구현 작성**

Create `backend/src/main/java/com/dddblog/backend/blog/domain/PostId.java`:

```java
package com.dddblog.backend.blog.domain;

import java.util.Objects;

public final class PostId {

	private final Long value;

	public PostId(Long value) {
		if (value == null || value < 1) {
			throw new IllegalArgumentException("Post id must be positive.");
		}
		this.value = value;
	}

	public Long value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof PostId postId)) {
			return false;
		}
		return Objects.equals(value, postId.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
```

- [ ] **Step 4: 통과 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostIdTest
```

Expected:

- `BUILD SUCCESSFUL`
- 4 tests pass.

- [ ] **Step 5: 커밋**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/test/java/com/dddblog/backend/blog/domain/PostIdTest.java backend/src/main/java/com/dddblog/backend/blog/domain/PostId.java
git commit -m "feat: add post id value object"
```

Expected:

- Commit succeeds.

---

### Task 2: 글 작성 애플리케이션 서비스 TDD

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostCommand.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/PostRepository.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostService.java`
- Create: `backend/src/test/java/com/dddblog/backend/blog/application/FakePostRepository.java`
- Create: `backend/src/test/java/com/dddblog/backend/blog/application/CreatePostServiceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `backend/src/test/java/com/dddblog/backend/blog/application/CreatePostServiceTest.java`:

```java
package com.dddblog.backend.blog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;

class CreatePostServiceTest {

	@Test
	void 유효한_요청이면_글을_저장하고_ID를_반환한다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"# DDD\n\n본문",
			"DDD 소개",
			List.of("ddd", "tdd"),
			PostStatus.DRAFT
		);

		PostId postId = service.create(command);

		assertThat(postId).isEqualTo(new PostId(1L));
		assertThat(postRepository.savedPosts()).hasSize(1);
	}

	@Test
	void 저장된_글은_요청_값을_도메인_값으로_가진다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"# DDD\n\n본문",
			"DDD 소개",
			List.of("DDD", "TDD"),
			PostStatus.PUBLISHED
		);

		service.create(command);

		Post savedPost = postRepository.savedPosts().get(0);
		assertThat(savedPost.authorId().value()).isEqualTo(1L);
		assertThat(savedPost.title().value()).isEqualTo("DDD 시작하기");
		assertThat(savedPost.content().value()).isEqualTo("# DDD\n\n본문");
		assertThat(savedPost.summary().value()).isEqualTo("DDD 소개");
		assertThat(savedPost.tags()).extracting(tag -> tag.value()).containsExactly("ddd", "tdd");
		assertThat(savedPost.status()).isEqualTo(PostStatus.PUBLISHED);
	}

	@Test
	void 요약이_null이면_빈_요약으로_저장한다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"본문",
			null,
			List.of(),
			PostStatus.DRAFT
		);

		service.create(command);

		assertThat(postRepository.savedPosts().get(0).summary().value()).isEmpty();
	}

	@Test
	void 태그_목록이_null이면_빈_태그_목록으로_저장한다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"본문",
			"요약",
			null,
			PostStatus.DRAFT
		);

		service.create(command);

		assertThat(postRepository.savedPosts().get(0).tags()).isEmpty();
	}

	@Test
	void 태그가_10개를_초과하면_저장하지_않는다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"본문",
			"요약",
			List.of("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tag10", "tag11"),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> service.create(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post tags must be 10 or less.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}

	@Test
	void 중복된_태그가_있으면_저장하지_않는다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"본문",
			"요약",
			List.of("Java", "java"),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> service.create(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post tags must not be duplicated.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}

	@Test
	void 제목이_blank이면_저장하지_않는다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"   ",
			"본문",
			"요약",
			List.of(),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> service.create(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must not be blank.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}

	@Test
	void 본문이_blank이면_저장하지_않는다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);
		CreatePostCommand command = new CreatePostCommand(
			1L,
			"DDD 시작하기",
			"   ",
			"요약",
			List.of(),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> service.create(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content must not be blank.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}

	@Test
	void command가_null이면_저장하지_않는다() {
		FakePostRepository postRepository = new FakePostRepository();
		CreatePostService service = new CreatePostService(postRepository);

		assertThatThrownBy(() -> service.create(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Create post command must not be null.");
		assertThat(postRepository.savedPosts()).isEmpty();
	}
}
```

- [ ] **Step 2: 실패 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.application.CreatePostServiceTest
```

Expected:

- FAIL
- Compilation errors mentioning missing `CreatePostService`, `CreatePostCommand`, `FakePostRepository`, or application package classes.

- [ ] **Step 3: 애플리케이션 command 작성**

Create `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostCommand.java`:

```java
package com.dddblog.backend.blog.application;

import java.util.List;

import com.dddblog.backend.blog.domain.PostStatus;

public record CreatePostCommand(
	Long authorId,
	String title,
	String content,
	String summary,
	List<String> tags,
	PostStatus status
) {
}
```

- [ ] **Step 4: Repository 포트 작성**

Create `backend/src/main/java/com/dddblog/backend/blog/application/PostRepository.java`:

```java
package com.dddblog.backend.blog.application;

import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostId;

public interface PostRepository {

	PostId save(Post post);
}
```

- [ ] **Step 5: 테스트 fake repository 작성**

Create `backend/src/test/java/com/dddblog/backend/blog/application/FakePostRepository.java`:

```java
package com.dddblog.backend.blog.application;

import java.util.ArrayList;
import java.util.List;

import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostId;

class FakePostRepository implements PostRepository {

	private final List<Post> savedPosts = new ArrayList<>();

	@Override
	public PostId save(Post post) {
		savedPosts.add(post);
		return new PostId((long) savedPosts.size());
	}

	List<Post> savedPosts() {
		return List.copyOf(savedPosts);
	}
}
```

- [ ] **Step 6: 서비스 구현 작성**

Create `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostService.java`:

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

- [ ] **Step 7: 통과 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.application.CreatePostServiceTest
```

Expected:

- `BUILD SUCCESSFUL`
- 9 tests pass.

- [ ] **Step 8: 커밋**

Run:

```powershell
cd C:\dev\study\ddd-blog
git add backend/src/main/java/com/dddblog/backend/blog/application backend/src/test/java/com/dddblog/backend/blog/application
git commit -m "feat: add create post service"
```

Expected:

- Commit succeeds.

---

### Task 3: 전체 검증

**Files:**
- No source changes expected.

- [ ] **Step 1: 전체 테스트 실행**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test
```

Expected:

- `BUILD SUCCESSFUL`

- [ ] **Step 2: 영문 테스트 메서드명 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected:

- No output.

- [ ] **Step 3: Spring/JPA annotation 침투 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table'
```

Expected:

- No output.

- [ ] **Step 4: Git 상태 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog
git status --short
git log --oneline --max-count=5
```

Expected:

- `git status --short` has no output.
- Recent commits include `feat: add create post service` and `feat: add post id value object`.

---

## Self-Review

### Spec Coverage

- `PostId` value object and tests: Task 1.
- `CreatePostCommand`: Task 2.
- `PostRepository` port: Task 2.
- `CreatePostService`: Task 2.
- `FakePostRepository`: Task 2.
- Service tests with fake repository: Task 2.
- Null command behavior: Task 2.
- Full test and convention verification: Task 3.
- No JPA/Spring/API/auth scope: Task 3 verification.

### Placeholder Scan

- 금지된 임시 표기나 모호한 구현 지시가 남아 있지 않다.
- Every code-changing step includes exact file content.
- Every verification step includes exact commands and expected results.

### Type Consistency

- `PostId`, `CreatePostCommand`, `CreatePostService`, `PostRepository`, and `FakePostRepository` names are consistent across test and implementation steps.
- `CreatePostService.create(CreatePostCommand command)` consistently returns `PostId`.
- `PostRepository.save(Post post)` consistently returns `PostId`.
- Exception messages in tests match implementation snippets exactly.
