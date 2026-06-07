# JPA Post Repository Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first JPA persistence adapter for the blog post creation repository port.

**Architecture:** Keep the current domain and application packages pure. Add a `blog.persistence` adapter package with JPA entities, Spring Data repositories, and an adapter that implements the existing `PostRepository` port. Use H2 and `@DataJpaTest` for fast repository integration feedback.

**Tech Stack:** Java 21, Spring Boot 3.5.0, Spring Data JPA, Hibernate, H2, JUnit 5, AssertJ, Gradle Kotlin DSL.

---

## File Structure

- Modify: `backend/build.gradle.kts`
  - Add H2 as a test runtime dependency.
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostEntity.java`
  - Map the `posts` table and own the post-to-tag join table.
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaTagEntity.java`
  - Map the `tags` table and enforce unique tag names.
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaPostRepository.java`
  - Spring Data repository for `JpaPostEntity`.
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaTagRepository.java`
  - Spring Data repository for lookup by normalized tag name.
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapter.java`
  - Implement the application `PostRepository` port.
- Create: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`
  - Verify adapter behavior through H2-backed JPA.

Do not modify:

- `backend/src/main/java/com/dddblog/backend/blog/domain/*`
- `backend/src/main/java/com/dddblog/backend/blog/application/PostRepository.java`
- `backend/src/main/java/com/dddblog/backend/blog/application/CreatePostService.java`

---

### Task 1: Add H2 Test Dependency

**Files:**
- Modify: `backend/build.gradle.kts`

- [ ] **Step 1: Add H2 dependency**

Add this line inside the existing `dependencies` block, near the other test dependencies:

```kotlin
testRuntimeOnly("com.h2database:h2")
```

The dependencies block should include this test runtime dependency:

```kotlin
dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.mysql:mysql-connector-j")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}
```

- [ ] **Step 2: Verify dependency resolution**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat dependencies --configuration testRuntimeClasspath
```

Expected: output contains `com.h2database:h2`.

- [ ] **Step 3: Commit**

```powershell
git add backend\build.gradle.kts
git commit -m "build: add h2 test dependency"
```

---

### Task 2: Write First Failing Persistence Test

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`

- [ ] **Step 1: Write failing test for ID return**

Create `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`:

```java
package com.dddblog.backend.blog.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

@DataJpaTest
@Import(JpaPostRepositoryAdapter.class)
class JpaPostRepositoryAdapterTest {

	@Autowired
	private JpaPostRepositoryAdapter postRepository;

	@Test
	void 글을_저장하면_ID를_반환한다() {
		Post post = new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("# DDD\n\n본문"),
			new PostSummary("DDD 소개"),
			List.of(new TagName("ddd"), new TagName("tdd")),
			PostStatus.DRAFT
		);

		PostId postId = postRepository.save(post);

		assertThat(postId.value()).isPositive();
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "com.dddblog.backend.blog.persistence.JpaPostRepositoryAdapterTest"
```

Expected: FAIL because `JpaPostRepositoryAdapter` does not exist.

---

### Task 3: Add Minimal JPA Entities And Spring Data Repositories

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaTagEntity.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostEntity.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaPostRepository.java`
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaTagRepository.java`

- [ ] **Step 1: Create tag entity**

Create `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaTagEntity.java`:

```java
package com.dddblog.backend.blog.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tags")
class JpaTagEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30, unique = true)
	private String name;

	protected JpaTagEntity() {
	}

	JpaTagEntity(String name) {
		this.name = name;
	}

	Long id() {
		return id;
	}

	String name() {
		return name;
	}
}
```

- [ ] **Step 2: Create post entity**

Create `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostEntity.java`:

```java
package com.dddblog.backend.blog.persistence;

import java.util.LinkedHashSet;
import java.util.Set;

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
		inverseJoinColumns = @JoinColumn(name = "tag_id")
	)
	private Set<JpaTagEntity> tags = new LinkedHashSet<>();

	protected JpaPostEntity() {
	}

	JpaPostEntity(
		Long authorId,
		String title,
		String contentMarkdown,
		String summary,
		PostStatus status,
		Set<JpaTagEntity> tags
	) {
		this.authorId = authorId;
		this.title = title;
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

- [ ] **Step 3: Create post Spring Data repository**

Create `backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaPostRepository.java`:

```java
package com.dddblog.backend.blog.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJpaPostRepository extends JpaRepository<JpaPostEntity, Long> {
}
```

- [ ] **Step 4: Create tag Spring Data repository**

Create `backend/src/main/java/com/dddblog/backend/blog/persistence/SpringDataJpaTagRepository.java`:

```java
package com.dddblog.backend.blog.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJpaTagRepository extends JpaRepository<JpaTagEntity, Long> {

	Optional<JpaTagEntity> findByName(String name);
}
```

- [ ] **Step 5: Run failing adapter test again**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "com.dddblog.backend.blog.persistence.JpaPostRepositoryAdapterTest"
```

Expected: FAIL because `JpaPostRepositoryAdapter` does not exist.

---

### Task 4: Implement Minimal Adapter Save

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapter.java`

- [ ] **Step 1: Create adapter implementation**

Create `backend/src/main/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapter.java`:

```java
package com.dddblog.backend.blog.persistence;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.dddblog.backend.blog.application.PostRepository;
import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.TagName;

@Repository
public class JpaPostRepositoryAdapter implements PostRepository {

	private final SpringDataJpaPostRepository postRepository;
	private final SpringDataJpaTagRepository tagRepository;

	public JpaPostRepositoryAdapter(
		SpringDataJpaPostRepository postRepository,
		SpringDataJpaTagRepository tagRepository
	) {
		this.postRepository = postRepository;
		this.tagRepository = tagRepository;
	}

	@Override
	public PostId save(Post post) {
		if (post == null) {
			throw new IllegalArgumentException("Post must not be null.");
		}
		Set<JpaTagEntity> tags = findOrCreateTags(post);
		JpaPostEntity entity = new JpaPostEntity(
			post.authorId().value(),
			post.title().value(),
			post.content().value(),
			post.summary().value(),
			post.status(),
			tags
		);
		JpaPostEntity savedEntity = postRepository.save(entity);
		return new PostId(savedEntity.id());
	}

	private Set<JpaTagEntity> findOrCreateTags(Post post) {
		Set<JpaTagEntity> tags = new LinkedHashSet<>();
		for (TagName tagName : post.tags()) {
			JpaTagEntity tag = tagRepository.findByName(tagName.value())
				.orElseGet(() -> tagRepository.save(new JpaTagEntity(tagName.value())));
			tags.add(tag);
		}
		return tags;
	}
}
```

- [ ] **Step 2: Run test to verify it passes**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "com.dddblog.backend.blog.persistence.JpaPostRepositoryAdapterTest"
```

Expected: PASS.

- [ ] **Step 3: Commit**

```powershell
git add backend\src\main\java\com\dddblog\backend\blog\persistence backend\src\test\java\com\dddblog\backend\blog\persistence
git commit -m "feat: add jpa post repository adapter"
```

---

### Task 5: Verify Stored Post Values

**Files:**
- Modify: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`

- [ ] **Step 1: Add repository fields and stored value test**

Replace the test file with:

```java
package com.dddblog.backend.blog.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;

@DataJpaTest
@Import(JpaPostRepositoryAdapter.class)
class JpaPostRepositoryAdapterTest {

	@Autowired
	private JpaPostRepositoryAdapter postRepository;

	@Autowired
	private SpringDataJpaPostRepository springDataPostRepository;

	@Test
	void 글을_저장하면_ID를_반환한다() {
		Post post = createPost();

		PostId postId = postRepository.save(post);

		assertThat(postId.value()).isPositive();
	}

	@Test
	void 글을_저장하면_본문_값이_posts에_저장된다() {
		Post post = createPost();

		PostId postId = postRepository.save(post);

		JpaPostEntity savedPost = springDataPostRepository.findById(postId.value()).orElseThrow();
		assertThat(savedPost.authorId()).isEqualTo(1L);
		assertThat(savedPost.title()).isEqualTo("DDD 시작하기");
		assertThat(savedPost.contentMarkdown()).isEqualTo("# DDD\n\n본문");
		assertThat(savedPost.summary()).isEqualTo("DDD 소개");
		assertThat(savedPost.status()).isEqualTo(PostStatus.DRAFT);
		assertThat(savedPost.tags()).extracting(JpaTagEntity::name).containsExactlyInAnyOrder("ddd", "tdd");
	}

	private Post createPost() {
		return new Post(
			new AuthorId(1L),
			new PostTitle("DDD 시작하기"),
			new PostContent("# DDD\n\n본문"),
			new PostSummary("DDD 소개"),
			List.of(new TagName("ddd"), new TagName("tdd")),
			PostStatus.DRAFT
		);
	}
}
```

- [ ] **Step 2: Run test to verify it passes**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "com.dddblog.backend.blog.persistence.JpaPostRepositoryAdapterTest"
```

Expected: PASS.

- [ ] **Step 3: Commit**

```powershell
git add backend\src\test\java\com\dddblog\backend\blog\persistence\JpaPostRepositoryAdapterTest.java
git commit -m "test: verify persisted post values"
```

---

### Task 6: Verify Existing Tag Reuse

**Files:**
- Modify: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`

- [ ] **Step 1: Add tag repository field and tag reuse test**

Add this field in `JpaPostRepositoryAdapterTest`:

```java
	@Autowired
	private SpringDataJpaTagRepository springDataTagRepository;
```

Add this test method:

```java
	@Test
	void 이미_존재하는_태그는_새로_만들지_않고_재사용한다() {
		springDataTagRepository.save(new JpaTagEntity("ddd"));
		Post firstPost = createPost();
		Post secondPost = new Post(
			new AuthorId(2L),
			new PostTitle("JPA 시작하기"),
			new PostContent("JPA 본문"),
			new PostSummary("JPA 소개"),
			List.of(new TagName("DDD")),
			PostStatus.PUBLISHED
		);

		postRepository.save(firstPost);
		postRepository.save(secondPost);

		assertThat(springDataTagRepository.findAll())
			.extracting(JpaTagEntity::name)
			.containsExactlyInAnyOrder("ddd", "tdd");
	}
```

- [ ] **Step 2: Run test to verify it passes**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "com.dddblog.backend.blog.persistence.JpaPostRepositoryAdapterTest"
```

Expected: PASS.

- [ ] **Step 3: Commit**

```powershell
git add backend\src\test\java\com\dddblog\backend\blog\persistence\JpaPostRepositoryAdapterTest.java
git commit -m "test: verify post tag reuse"
```

---

### Task 7: Verify Null Post Rejection

**Files:**
- Modify: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`

- [ ] **Step 1: Add AssertJ thrown-by import**

Add this import:

```java
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

- [ ] **Step 2: Add null post test**

Add this test method:

```java
	@Test
	void 글이_null이면_저장할_수_없다() {
		assertThatThrownBy(() -> postRepository.save(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post must not be null.");
		assertThat(springDataPostRepository.findAll()).isEmpty();
		assertThat(springDataTagRepository.findAll()).isEmpty();
	}
```

- [ ] **Step 3: Run test to verify it passes**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "com.dddblog.backend.blog.persistence.JpaPostRepositoryAdapterTest"
```

Expected: PASS.

- [ ] **Step 4: Commit**

```powershell
git add backend\src\test\java\com\dddblog\backend\blog\persistence\JpaPostRepositoryAdapterTest.java
git commit -m "test: reject null post persistence"
```

---

### Task 8: Run Full Verification

**Files:**
- No code changes expected.

- [ ] **Step 1: Run all backend tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Verify Korean test method naming**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected: no output.

- [ ] **Step 3: Verify pure domain/application packages remain annotation-free**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table'
```

Expected: no output.

- [ ] **Step 4: Check working tree**

Run:

```powershell
cd C:\dev\study\ddd-blog
git status --short
```

Expected: no output.

If there is uncommitted implementation work, commit it with:

```powershell
git add backend\build.gradle.kts backend\src\main\java\com\dddblog\backend\blog\persistence backend\src\test\java\com\dddblog\backend\blog\persistence
git commit -m "feat: add jpa post repository persistence"
```

---

## Self-Review

Spec coverage:

- H2 test dependency is covered by Task 1.
- Separate persistence package is covered by Tasks 3 and 4.
- `PostRepository` port implementation is covered by Task 4.
- ID-less domain `Post` is preserved because no domain files are modified.
- `tags` and `post_tags` normalized mapping is covered by Task 3.
- H2-backed `@DataJpaTest` is covered by Tasks 2, 5, 6, and 7.
- Null post rejection is covered by Task 7.
- Full verification and package annotation checks are covered by Task 8.

Placeholder scan:

- No placeholder markers or unspecified implementation steps remain.

Type consistency:

- `JpaPostRepositoryAdapter.save(Post)` matches the existing `PostRepository` port.
- Test helper code uses existing value object constructors and accessors.
- Spring Data repository names match adapter constructor dependencies.
