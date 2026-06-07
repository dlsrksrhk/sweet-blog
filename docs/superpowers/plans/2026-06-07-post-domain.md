# Post Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 글 작성 도메인의 1차 순수 도메인 모델과 테스트를 구현하고, 댓글 요구사항과 한글 테스트명 규칙을 프로젝트 문서에 반영한다.

**Architecture:** 이번 작업은 Spring/JPA/API를 사용하지 않는 순수 Java 도메인 계층만 다룬다. 값 객체가 입력 검증과 정규화를 담당하고, `Post` aggregate가 글 생성 시점의 조합 규칙과 태그 개수 규칙을 담당한다. 댓글은 요구사항 문서에만 추가하고 구현은 별도 단계로 분리한다.

**Tech Stack:** Java 21, Spring Boot 3.5.0, Gradle Kotlin DSL, JUnit 5, AssertJ

---

## File Structure

- Create: `AGENTS.md`
  - 프로젝트 로컬 규칙을 문서화한다.
  - 백엔드 테스트 메서드명을 한글 시나리오형으로 작성한다는 규칙을 추가한다.
- Modify: `docs/requirements.md`
  - 댓글 작성/수정 요구사항을 추가한다.
  - 초기 제외 범위에서 댓글을 제거한다.
- Modify: `backend/src/test/java/com/dddblog/backend/blog/domain/PostTitleTest.java`
  - 기존 테스트 메서드명을 한글로 변경한다.
- Create: `backend/src/test/java/com/dddblog/backend/blog/domain/PostContentTest.java`
  - Markdown 본문 값 객체의 검증 규칙을 테스트한다.
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/PostContent.java`
  - Markdown 본문 값 객체를 구현한다.
- Create: `backend/src/test/java/com/dddblog/backend/blog/domain/PostSummaryTest.java`
  - 요약 값 객체의 정규화와 길이 제한을 테스트한다.
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/PostSummary.java`
  - 요약 값 객체를 구현한다.
- Create: `backend/src/test/java/com/dddblog/backend/blog/domain/TagNameTest.java`
  - 태그명 값 객체의 검증과 정규화를 테스트한다.
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/TagName.java`
  - 태그명 값 객체를 구현한다.
- Create: `backend/src/test/java/com/dddblog/backend/blog/domain/AuthorIdTest.java`
  - 작성자 ID 값 객체의 null 거부 규칙을 테스트한다.
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/AuthorId.java`
  - 작성자 ID 값 객체를 구현한다.
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/PostStatus.java`
  - 글 상태 enum을 추가한다.
- Create: `backend/src/test/java/com/dddblog/backend/blog/domain/PostTest.java`
  - `Post` aggregate 생성 규칙과 태그 컬렉션 보호를 테스트한다.
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/Post.java`
  - 글 aggregate를 구현한다.

---

### Task 1: 프로젝트 규칙과 댓글 요구사항 문서 반영

**Files:**
- Create: `AGENTS.md`
- Modify: `docs/requirements.md`

- [ ] **Step 1: `AGENTS.md`에 프로젝트 규칙 추가**

Create `AGENTS.md`:

```markdown
# AGENTS.md instructions for C:\dev\study\ddd-blog

## Project Rules

- 백엔드 테스트 메서드명은 IntelliJ 테스트 결과 목록에서 한국어 행동 설명으로 읽히도록 한글 시나리오형으로 작성한다.
- 한글 테스트 메서드명은 단어 사이를 `_`로 연결한다.
- 예: `본문이_비어_있으면_생성할_수_없다`
- 순수 도메인 테스트는 Spring Context를 띄우지 않는다.
- 도메인 클래스에는 Spring annotation을 붙이지 않는다.
- 도메인 규칙은 가능한 한 값 객체와 aggregate 안에 둔다.
- 이번 프로젝트는 DDD와 TDD 학습용이므로 작은 실패 테스트를 먼저 만들고 최소 구현으로 통과시킨다.
```

- [ ] **Step 2: `docs/requirements.md`에 댓글 기능 요구사항 추가**

In `docs/requirements.md`, after section `5.14 검색`, add:

```markdown
## 5.15 댓글

사용자는 공개된 글에 댓글을 작성할 수 있다.

### 댓글 작성자 유형

- 회원 댓글: 로그인한 회원이 작성한 댓글
- 익명 댓글: 로그인하지 않은 사용자가 닉네임과 임시 비밀번호를 입력해 작성한 댓글

### 입력 항목

#### 회원 댓글

- 본문

#### 익명 댓글

- 닉네임
- 임시 비밀번호
- 본문

### 규칙

- 댓글은 특정 글 하나에 속한다.
- 댓글 본문은 비어 있을 수 없다.
- 회원 댓글은 작성한 회원만 수정할 수 있다.
- 익명 댓글은 작성 시 입력한 임시 비밀번호가 맞을 때만 수정할 수 있다.
- 익명 댓글의 임시 비밀번호는 평문으로 저장하지 않고 해시로 저장한다.
- 초기 버전에서는 댓글 삭제, 대댓글, 신고, 관리자 숨김 처리를 제외한다.

### 성공 결과

- 댓글이 생성된다.
- 생성된 댓글은 글 상세 화면에서 조회할 수 있다.

### 실패 조건

- 댓글 본문이 비어 있다.
- 익명 댓글 작성 시 닉네임이 비어 있다.
- 익명 댓글 작성 시 임시 비밀번호가 비어 있다.
- 회원 댓글 수정 요청자가 작성자가 아니다.
- 익명 댓글 수정 시 임시 비밀번호가 일치하지 않는다.
```

- [ ] **Step 3: 초기 제외 범위에서 댓글 제거**

In `docs/requirements.md`, section `13. 제외 범위`, remove this bullet:

```markdown
- 댓글
```

- [ ] **Step 4: 문서 변경 확인**

Run:

```powershell
Select-String -Path .\docs\requirements.md -Pattern '## 5.15 댓글|회원 댓글|익명 댓글|임시 비밀번호'
Select-String -Path .\docs\requirements.md -Pattern '^- 댓글$'
```

Expected:

- First command prints matches for the new comment requirements.
- Second command prints no result.

---

### Task 2: `PostTitleTest` 한글 테스트명 변경

**Files:**
- Modify: `backend/src/test/java/com/dddblog/backend/blog/domain/PostTitleTest.java`

- [ ] **Step 1: 기존 테스트 메서드명을 한글로 변경**

Replace the entire file with:

```java
package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostTitleTest {

	@Test
	void 제목을_생성한다() {
		PostTitle title = new PostTitle("DDD and TDD blog");

		assertThat(title.value()).isEqualTo("DDD and TDD blog");
	}

	@Test
	void 제목이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostTitle(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must not be blank.");
	}

	@Test
	void 제목이_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostTitle("   "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must not be blank.");
	}

	@Test
	void 제목이_100자를_초과하면_생성할_수_없다() {
		String title = "a".repeat(101);

		assertThatThrownBy(() -> new PostTitle(title))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must be 100 characters or less.");
	}

	@Test
	void 제목은_100자까지_허용한다() {
		String title = "a".repeat(100);

		PostTitle postTitle = new PostTitle(title);

		assertThat(postTitle.value()).isEqualTo(title);
	}
}
```

- [ ] **Step 2: 기존 테스트가 계속 통과하는지 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostTitleTest
```

Expected:

- `BUILD SUCCESSFUL`
- 5 tests pass.

---

### Task 3: `PostContent` 값 객체 TDD

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/blog/domain/PostContentTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/PostContent.java`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `PostContentTest.java`:

```java
package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostContentTest {

	@Test
	void 본문을_생성한다() {
		PostContent content = new PostContent("# 제목\n\n본문입니다.");

		assertThat(content.value()).isEqualTo("# 제목\n\n본문입니다.");
	}

	@Test
	void 본문이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostContent(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content must not be blank.");
	}

	@Test
	void 본문이_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PostContent("   "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content must not be blank.");
	}
}
```

- [ ] **Step 2: 실패 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostContentTest
```

Expected:

- FAIL
- Compilation error mentioning `cannot find symbol class PostContent`.

- [ ] **Step 3: 최소 구현 작성**

Create `PostContent.java`:

```java
package com.dddblog.backend.blog.domain;

import java.util.Objects;

public final class PostContent {

	private final String value;

	public PostContent(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Post content must not be blank.");
		}
		this.value = value;
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof PostContent postContent)) {
			return false;
		}
		return Objects.equals(value, postContent.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return value;
	}
}
```

- [ ] **Step 4: 통과 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostContentTest
```

Expected:

- `BUILD SUCCESSFUL`
- 3 tests pass.

---

### Task 4: `PostSummary` 값 객체 TDD

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/blog/domain/PostSummaryTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/PostSummary.java`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `PostSummaryTest.java`:

```java
package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostSummaryTest {

	@Test
	void 요약을_생성한다() {
		PostSummary summary = new PostSummary("짧은 소개");

		assertThat(summary.value()).isEqualTo("짧은 소개");
	}

	@Test
	void 요약이_null이면_빈_요약으로_생성한다() {
		PostSummary summary = new PostSummary(null);

		assertThat(summary.value()).isEmpty();
	}

	@Test
	void 요약이_blank이면_빈_요약으로_생성한다() {
		PostSummary summary = new PostSummary("   ");

		assertThat(summary.value()).isEmpty();
	}

	@Test
	void 요약은_앞뒤_공백을_제거한다() {
		PostSummary summary = new PostSummary("  짧은 소개  ");

		assertThat(summary.value()).isEqualTo("짧은 소개");
	}

	@Test
	void 요약이_300자를_초과하면_생성할_수_없다() {
		String summary = "a".repeat(301);

		assertThatThrownBy(() -> new PostSummary(summary))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post summary must be 300 characters or less.");
	}

	@Test
	void 요약은_300자까지_허용한다() {
		String summary = "a".repeat(300);

		PostSummary postSummary = new PostSummary(summary);

		assertThat(postSummary.value()).isEqualTo(summary);
	}
}
```

- [ ] **Step 2: 실패 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostSummaryTest
```

Expected:

- FAIL
- Compilation error mentioning `cannot find symbol class PostSummary`.

- [ ] **Step 3: 최소 구현 작성**

Create `PostSummary.java`:

```java
package com.dddblog.backend.blog.domain;

import java.util.Objects;

public final class PostSummary {

	private static final int MAX_LENGTH = 300;

	private final String value;

	public PostSummary(String value) {
		String normalized = normalize(value);
		if (normalized.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Post summary must be 300 characters or less.");
		}
		this.value = normalized;
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		return value.trim();
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof PostSummary postSummary)) {
			return false;
		}
		return Objects.equals(value, postSummary.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return value;
	}
}
```

- [ ] **Step 4: 통과 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostSummaryTest
```

Expected:

- `BUILD SUCCESSFUL`
- 6 tests pass.

---

### Task 5: `TagName` 값 객체 TDD

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/blog/domain/TagNameTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/TagName.java`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `TagNameTest.java`:

```java
package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TagNameTest {

	@Test
	void 태그명을_생성한다() {
		TagName tagName = new TagName("ddd");

		assertThat(tagName.value()).isEqualTo("ddd");
	}

	@Test
	void 태그명은_앞뒤_공백을_제거하고_소문자로_정규화한다() {
		TagName tagName = new TagName("  Java  ");

		assertThat(tagName.value()).isEqualTo("java");
	}

	@Test
	void 태그명이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new TagName(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Tag name must not be blank.");
	}

	@Test
	void 태그명이_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new TagName("   "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Tag name must not be blank.");
	}

	@Test
	void 태그명이_30자를_초과하면_생성할_수_없다() {
		String tagName = "a".repeat(31);

		assertThatThrownBy(() -> new TagName(tagName))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Tag name must be 30 characters or less.");
	}

	@Test
	void 태그명은_30자까지_허용한다() {
		String tagName = "a".repeat(30);

		TagName created = new TagName(tagName);

		assertThat(created.value()).isEqualTo(tagName);
	}
}
```

- [ ] **Step 2: 실패 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.TagNameTest
```

Expected:

- FAIL
- Compilation error mentioning `cannot find symbol class TagName`.

- [ ] **Step 3: 최소 구현 작성**

Create `TagName.java`:

```java
package com.dddblog.backend.blog.domain;

import java.util.Locale;
import java.util.Objects;

public final class TagName {

	private static final int MAX_LENGTH = 30;

	private final String value;

	public TagName(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Tag name must not be blank.");
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		if (normalized.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Tag name must be 30 characters or less.");
		}
		this.value = normalized;
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof TagName tagName)) {
			return false;
		}
		return Objects.equals(value, tagName.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return value;
	}
}
```

- [ ] **Step 4: 통과 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.TagNameTest
```

Expected:

- `BUILD SUCCESSFUL`
- 6 tests pass.

---

### Task 6: `AuthorId` 값 객체 TDD

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/blog/domain/AuthorIdTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/AuthorId.java`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `AuthorIdTest.java`:

```java
package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AuthorIdTest {

	@Test
	void 작성자_ID를_생성한다() {
		AuthorId authorId = new AuthorId(1L);

		assertThat(authorId.value()).isEqualTo(1L);
	}

	@Test
	void 작성자_ID가_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new AuthorId(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Author id must not be null.");
	}
}
```

- [ ] **Step 2: 실패 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.AuthorIdTest
```

Expected:

- FAIL
- Compilation error mentioning `cannot find symbol class AuthorId`.

- [ ] **Step 3: 최소 구현 작성**

Create `AuthorId.java`:

```java
package com.dddblog.backend.blog.domain;

import java.util.Objects;

public final class AuthorId {

	private final Long value;

	public AuthorId(Long value) {
		if (value == null) {
			throw new IllegalArgumentException("Author id must not be null.");
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
		if (!(object instanceof AuthorId authorId)) {
			return false;
		}
		return Objects.equals(value, authorId.value);
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
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.AuthorIdTest
```

Expected:

- `BUILD SUCCESSFUL`
- 2 tests pass.

---

### Task 7: `PostStatus` enum 추가

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/PostStatus.java`

- [ ] **Step 1: enum 작성**

Create `PostStatus.java`:

```java
package com.dddblog.backend.blog.domain;

public enum PostStatus {
	DRAFT,
	PUBLISHED,
	HIDDEN
}
```

- [ ] **Step 2: 컴파일 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat compileJava
```

Expected:

- `BUILD SUCCESSFUL`

---

### Task 8: `Post` aggregate TDD

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/blog/domain/PostTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/domain/Post.java`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `PostTest.java`:

```java
package com.dddblog.backend.blog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class PostTest {

	@Test
	void 글을_생성한다() {
		AuthorId authorId = new AuthorId(1L);
		PostTitle title = new PostTitle("DDD 시작하기");
		PostContent content = new PostContent("# DDD\n\n본문");
		PostSummary summary = new PostSummary("DDD 소개");
		List<TagName> tags = List.of(new TagName("ddd"), new TagName("tdd"));

		Post post = new Post(authorId, title, content, summary, tags, PostStatus.DRAFT);

		assertThat(post.authorId()).isEqualTo(authorId);
		assertThat(post.title()).isEqualTo(title);
		assertThat(post.content()).isEqualTo(content);
		assertThat(post.summary()).isEqualTo(summary);
		assertThat(post.tags()).containsExactlyElementsOf(tags);
		assertThat(post.status()).isEqualTo(PostStatus.DRAFT);
	}

	@Test
	void 작성자가_없으면_글을_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			null,
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post author must not be null.");
	}

	@Test
	void 제목이_없으면_글을_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			null,
			new PostContent("본문"),
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post title must not be null.");
	}

	@Test
	void 본문이_없으면_글을_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			null,
			new PostSummary("요약"),
			List.of(),
			PostStatus.DRAFT
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post content must not be null.");
	}

	@Test
	void 상태가_없으면_글을_생성할_수_없다() {
		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			List.of(),
			null
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post status must not be null.");
	}

	@Test
	void 요약이_없으면_빈_요약으로_글을_생성한다() {
		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			null,
			List.of(),
			PostStatus.DRAFT
		);

		assertThat(post.summary().value()).isEmpty();
	}

	@Test
	void 태그_목록이_null이면_빈_태그_목록으로_글을_생성한다() {
		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			null,
			PostStatus.DRAFT
		);

		assertThat(post.tags()).isEmpty();
	}

	@Test
	void 태그는_10개까지_허용한다() {
		List<TagName> tags = List.of(
			new TagName("tag1"),
			new TagName("tag2"),
			new TagName("tag3"),
			new TagName("tag4"),
			new TagName("tag5"),
			new TagName("tag6"),
			new TagName("tag7"),
			new TagName("tag8"),
			new TagName("tag9"),
			new TagName("tag10")
		);

		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			tags,
			PostStatus.DRAFT
		);

		assertThat(post.tags()).hasSize(10);
	}

	@Test
	void 태그가_10개를_초과하면_글을_생성할_수_없다() {
		List<TagName> tags = List.of(
			new TagName("tag1"),
			new TagName("tag2"),
			new TagName("tag3"),
			new TagName("tag4"),
			new TagName("tag5"),
			new TagName("tag6"),
			new TagName("tag7"),
			new TagName("tag8"),
			new TagName("tag9"),
			new TagName("tag10"),
			new TagName("tag11")
		);

		assertThatThrownBy(() -> new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			tags,
			PostStatus.DRAFT
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post tags must be 10 or less.");
	}

	@Test
	void 외부에_노출된_태그_목록은_수정할_수_없다() {
		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("본문"),
			new PostSummary("요약"),
			List.of(new TagName("ddd")),
			PostStatus.DRAFT
		);

		assertThatThrownBy(() -> post.tags().add(new TagName("tdd")))
			.isInstanceOf(UnsupportedOperationException.class);
	}
}
```

- [ ] **Step 2: 실패 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostTest
```

Expected:

- FAIL
- Compilation error mentioning `cannot find symbol class Post`.

- [ ] **Step 3: 최소 구현 작성**

Create `Post.java`:

```java
package com.dddblog.backend.blog.domain;

import java.util.List;

public final class Post {

	private static final int MAX_TAG_COUNT = 10;

	private final AuthorId authorId;
	private final PostTitle title;
	private final PostContent content;
	private final PostSummary summary;
	private final List<TagName> tags;
	private final PostStatus status;

	public Post(
		AuthorId authorId,
		PostTitle title,
		PostContent content,
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
		this.summary = summary == null ? new PostSummary(null) : summary;
		this.tags = copiedTags;
		this.status = status;
	}

	private List<TagName> copyTags(List<TagName> tags) {
		if (tags == null) {
			return List.of();
		}
		return List.copyOf(tags);
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

- [ ] **Step 4: 통과 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.blog.domain.PostTest
```

Expected:

- `BUILD SUCCESSFUL`
- 10 tests pass.

---

### Task 9: 전체 테스트와 한글 테스트명 확인

**Files:**
- No file changes.

- [ ] **Step 1: 전체 테스트 실행**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test
```

Expected:

- `BUILD SUCCESSFUL`
- `PostTitleTest`, `PostContentTest`, `PostSummaryTest`, `TagNameTest`, `AuthorIdTest`, `PostTest` all pass.

- [ ] **Step 2: 영문 테스트 메서드명이 남아 있는지 확인**

Run:

```powershell
Select-String -Path .\src\test\java\com\dddblog\backend\blog\domain\*.java -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected:

- No output.

- [ ] **Step 3: 도메인 클래스에 Spring annotation이 들어갔는지 확인**

Run:

```powershell
Select-String -Path .\src\main\java\com\dddblog\backend\blog\domain\*.java -Pattern '@Component|@Service|@Entity|@Embeddable|@Table'
```

Expected:

- No output.

---

### Task 10: 커밋 처리

**Files:**
- All files changed in previous tasks.

- [ ] **Step 1: Git 저장소 여부 확인**

Run:

```powershell
cd C:\dev\study\ddd-blog
git status --short
```

Expected if this workspace is still not a Git repository:

```text
fatal: not a git repository (or any of the parent directories): .git
```

Expected if the workspace has been initialized as a Git repository:

```text
?? AGENTS.md
?? docs/superpowers/
...
```

- [ ] **Step 2: Git 저장소가 아니면 커밋을 건너뛰고 사용자에게 알린다**

Use this exact note in the completion summary:

```text
현재 C:\dev\study\ddd-blog는 Git 저장소가 아니라 커밋은 수행하지 않았습니다.
```

- [ ] **Step 3: Git 저장소라면 변경사항 커밋**

Run only if `git status --short` succeeds:

```powershell
git add AGENTS.md docs backend
git commit -m "feat: add initial post domain model"
```

Expected:

- Commit succeeds.

---

## Self-Review

### Spec Coverage

- 한글 테스트 메서드명 규칙: Task 1, Task 2, Task 9.
- 댓글 요구사항 문서 반영: Task 1.
- `PostContent`: Task 3.
- `PostSummary`: Task 4.
- `TagName`: Task 5.
- `AuthorId`: Task 6.
- `PostStatus`: Task 7.
- `Post` aggregate: Task 8.
- 전체 테스트 검증: Task 9.
- Git 저장소가 아닌 현재 상태 처리: Task 10.

### Placeholder Scan

- 금지된 임시 표기나 모호한 검증 지시가 남아 있지 않다.
- Every code-changing step includes exact file content or exact markdown insertion content.
- Every verification step includes exact commands and expected results.

### Type Consistency

- `AuthorId`, `PostTitle`, `PostContent`, `PostSummary`, `TagName`, `PostStatus`, and `Post` names are used consistently.
- Accessor methods are consistently named `value()` for value objects and `authorId()`, `title()`, `content()`, `summary()`, `tags()`, `status()` for `Post`.
- Exception messages in tests match implementation snippets exactly.
