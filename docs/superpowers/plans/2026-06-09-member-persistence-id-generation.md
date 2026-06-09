# Member Persistence ID Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first JPA-backed member repository, move member ID generation into a dedicated `MemberIdGenerator` port, and run all JPA repository tests on MySQL Testcontainers.

**Architecture:** Keep `member.domain` and `member.application` pure Java. Split ID generation from `MemberRepository` into `MemberIdGenerator`, implement member persistence in `member.persistence`, and use a DB row-backed sequence table for pre-save member IDs. Add a shared test support class for MySQL Testcontainers and migrate the existing post persistence test to the same database style.

**Tech Stack:** Java 21, Spring Boot 3.5.0, Spring Data JPA, Hibernate DDL in tests, MySQL Testcontainers, JUnit 5, AssertJ, Gradle Kotlin DSL.

---

## File Structure

- Create: `backend/src/main/java/com/dddblog/backend/member/application/MemberIdGenerator.java`
  - Application port for issuing pre-save `MemberId` values.
- Modify: `backend/src/main/java/com/dddblog/backend/member/application/MemberRepository.java`
  - Remove `nextId()` so the repository only handles persistence lookups and save.
- Modify: `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberService.java`
  - Inject and use `MemberIdGenerator`.
- Create: `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberIdGenerator.java`
  - Test fake for member ID generation.
- Modify: `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberRepository.java`
  - Remove fake ID generation.
- Modify: `backend/src/test/java/com/dddblog/backend/member/application/RegisterMemberServiceTest.java`
  - Construct service with repository and ID generator.
- Modify: `backend/build.gradle.kts`
  - Add Testcontainers dependencies and remove H2.
- Create: `backend/src/test/java/com/dddblog/backend/support/MysqlDataJpaTestSupport.java`
  - Shared MySQL container datasource setup for JPA tests.
- Modify: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`
  - Run existing post persistence scenarios on MySQL Testcontainers.
- Create: `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberEntity.java`
  - JPA entity for `members`.
- Create: `backend/src/main/java/com/dddblog/backend/member/persistence/SpringDataJpaMemberRepository.java`
  - Spring Data repository for member entity lookups.
- Create: `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapter.java`
  - Adapter implementing `MemberRepository`.
- Create: `backend/src/test/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapterTest.java`
  - MySQL-backed persistence tests for member save, lookup, unique constraints, and null guard.
- Create: `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberIdSequenceEntity.java`
  - JPA entity for `member_id_sequences`.
- Create: `backend/src/main/java/com/dddblog/backend/member/persistence/SpringDataJpaMemberIdSequenceRepository.java`
  - Spring Data repository with pessimistic lock lookup.
- Create: `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberIdGenerator.java`
  - DB row-backed `MemberIdGenerator`.
- Create: `backend/src/test/java/com/dddblog/backend/member/persistence/JpaMemberIdGeneratorTest.java`
  - MySQL-backed tests for ID generation.

Do not add REST API, BCrypt, JWT, Flyway/Liquibase, member read/update/delete methods, Spring configuration for `RegisterMemberService`, or a FK from `posts.author_id` to `members.id`.

---

### Task 1: Split Member ID Generation Port

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/member/application/MemberIdGenerator.java`
- Create: `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberIdGenerator.java`
- Modify: `backend/src/main/java/com/dddblog/backend/member/application/MemberRepository.java`
- Modify: `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberService.java`
- Modify: `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberRepository.java`
- Modify: `backend/src/test/java/com/dddblog/backend/member/application/RegisterMemberServiceTest.java`

- [ ] **Step 1: Write the failing application test update**

Replace `backend/src/test/java/com/dddblog/backend/member/application/RegisterMemberServiceTest.java` with:

```java
package com.dddblog.backend.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.MemberRole;
import com.dddblog.backend.member.domain.MemberStatus;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

class RegisterMemberServiceTest {

	@Test
	void 유효한_요청이면_회원을_저장하고_ID를_반환한다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		RegisterMemberService service = new RegisterMemberService(memberRepository, memberIdGenerator);
		RegisterMemberCommand command = validCommand();

		MemberId memberId = service.register(command);

		assertThat(memberId).isEqualTo(new MemberId(1L));
		assertThat(memberRepository.savedMembers()).hasSize(1);
	}

	@Test
	void 저장된_회원은_요청_값을_도메인_값으로_가진다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		RegisterMemberService service = new RegisterMemberService(memberRepository, memberIdGenerator);
		RegisterMemberCommand command = validCommand();

		service.register(command);

		Member savedMember = memberRepository.savedMembers().get(0);
		assertThat(savedMember.id()).isEqualTo(new MemberId(1L));
		assertThat(savedMember.name()).isEqualTo(new MemberName("홍길동"));
		assertThat(savedMember.nickname()).isEqualTo(new Nickname("길동"));
		assertThat(savedMember.loginId()).isEqualTo(new LoginId("user01"));
		assertThat(savedMember.passwordHash()).isEqualTo(new PasswordHash("$2a$10$hashed-password"));
	}

	@Test
	void 신규_회원은_MEMBER_권한과_ACTIVE_상태를_가진다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		RegisterMemberService service = new RegisterMemberService(memberRepository, memberIdGenerator);
		RegisterMemberCommand command = validCommand();

		service.register(command);

		Member savedMember = memberRepository.savedMembers().get(0);
		assertThat(savedMember.role()).isEqualTo(MemberRole.MEMBER);
		assertThat(savedMember.status()).isEqualTo(MemberStatus.ACTIVE);
	}

	@Test
	void command가_null이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		RegisterMemberService service = new RegisterMemberService(memberRepository, memberIdGenerator);

		assertThatThrownBy(() -> service.register(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Register member command must not be null.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 로그인_ID가_이미_존재하면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		memberRepository.addExistingLoginId(new LoginId("user01"));
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		RegisterMemberService service = new RegisterMemberService(memberRepository, memberIdGenerator);
		RegisterMemberCommand command = validCommand();

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id already exists.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 닉네임이_이미_존재하면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		memberRepository.addExistingNickname(new Nickname("길동"));
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		RegisterMemberService service = new RegisterMemberService(memberRepository, memberIdGenerator);
		RegisterMemberCommand command = validCommand();

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname already exists.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 잘못된_로그인_ID이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		RegisterMemberService service = new RegisterMemberService(memberRepository, memberIdGenerator);
		RegisterMemberCommand command = new RegisterMemberCommand(
			"홍길동",
			"길동",
			"abc",
			"$2a$10$hashed-password"
		);

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id must be 4 characters or more.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 잘못된_닉네임이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		RegisterMemberService service = new RegisterMemberService(memberRepository, memberIdGenerator);
		RegisterMemberCommand command = new RegisterMemberCommand(
			"홍길동",
			"길",
			"user01",
			"$2a$10$hashed-password"
		);

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname must be 2 characters or more.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 잘못된_비밀번호_해시이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		RegisterMemberService service = new RegisterMemberService(memberRepository, memberIdGenerator);
		RegisterMemberCommand command = new RegisterMemberCommand(
			"홍길동",
			"길동",
			"user01",
			" "
		);

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password hash must not be blank.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	private RegisterMemberCommand validCommand() {
		return new RegisterMemberCommand(
			"홍길동",
			"길동",
			"user01",
			"$2a$10$hashed-password"
		);
	}
}
```

- [ ] **Step 2: Run the application test to verify failure**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.member.application.RegisterMemberServiceTest
```

Expected: FAIL because `FakeMemberIdGenerator` does not exist and `RegisterMemberService` does not have the two-argument constructor.

- [ ] **Step 3: Add `MemberIdGenerator` and update application classes**

Create `backend/src/main/java/com/dddblog/backend/member/application/MemberIdGenerator.java`:

```java
package com.dddblog.backend.member.application;

import com.dddblog.backend.member.domain.MemberId;

public interface MemberIdGenerator {

	MemberId nextId();
}
```

Replace `backend/src/main/java/com/dddblog/backend/member/application/MemberRepository.java` with:

```java
package com.dddblog.backend.member.application;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;

public interface MemberRepository {

	boolean existsByLoginId(LoginId loginId);

	boolean existsByNickname(Nickname nickname);

	MemberId save(Member member);
}
```

Replace `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberService.java` with:

```java
package com.dddblog.backend.member.application;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

public class RegisterMemberService {

	private final MemberRepository memberRepository;
	private final MemberIdGenerator memberIdGenerator;

	public RegisterMemberService(MemberRepository memberRepository, MemberIdGenerator memberIdGenerator) {
		this.memberRepository = memberRepository;
		this.memberIdGenerator = memberIdGenerator;
	}

	public MemberId register(RegisterMemberCommand command) {
		if (command == null) {
			throw new IllegalArgumentException("Register member command must not be null.");
		}

		MemberName name = new MemberName(command.name());
		Nickname nickname = new Nickname(command.nickname());
		LoginId loginId = new LoginId(command.loginId());
		PasswordHash passwordHash = new PasswordHash(command.passwordHash());
		if (memberRepository.existsByLoginId(loginId)) {
			throw new IllegalArgumentException("Login id already exists.");
		}
		if (memberRepository.existsByNickname(nickname)) {
			throw new IllegalArgumentException("Nickname already exists.");
		}

		MemberId memberId = memberIdGenerator.nextId();
		Member member = Member.register(
			memberId,
			name,
			nickname,
			loginId,
			passwordHash
		);
		return memberRepository.save(member);
	}
}
```

Create `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberIdGenerator.java`:

```java
package com.dddblog.backend.member.application;

import com.dddblog.backend.member.domain.MemberId;

class FakeMemberIdGenerator implements MemberIdGenerator {

	private long nextId = 1L;

	@Override
	public MemberId nextId() {
		return new MemberId(nextId++);
	}
}
```

Replace `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberRepository.java` with:

```java
package com.dddblog.backend.member.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;

class FakeMemberRepository implements MemberRepository {

	private final List<Member> savedMembers = new ArrayList<>();
	private final Set<LoginId> existingLoginIds = new HashSet<>();
	private final Set<Nickname> existingNicknames = new HashSet<>();

	@Override
	public boolean existsByLoginId(LoginId loginId) {
		return existingLoginIds.contains(loginId);
	}

	@Override
	public boolean existsByNickname(Nickname nickname) {
		return existingNicknames.contains(nickname);
	}

	@Override
	public MemberId save(Member member) {
		savedMembers.add(member);
		existingLoginIds.add(member.loginId());
		existingNicknames.add(member.nickname());
		return member.id();
	}

	List<Member> savedMembers() {
		return List.copyOf(savedMembers);
	}

	FakeMemberRepository addExistingLoginId(LoginId loginId) {
		existingLoginIds.add(loginId);
		return this;
	}

	FakeMemberRepository addExistingNickname(Nickname nickname) {
		existingNicknames.add(nickname);
		return this;
	}
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat test --tests com.dddblog.backend.member.application.RegisterMemberServiceTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run from repository root:

```powershell
cd C:\dev\study\ddd-blog
git add backend\src\main\java\com\dddblog\backend\member\application\MemberIdGenerator.java backend\src\main\java\com\dddblog\backend\member\application\MemberRepository.java backend\src\main\java\com\dddblog\backend\member\application\RegisterMemberService.java backend\src\test\java\com\dddblog\backend\member\application\FakeMemberIdGenerator.java backend\src\test\java\com\dddblog\backend\member\application\FakeMemberRepository.java backend\src\test\java\com\dddblog\backend\member\application\RegisterMemberServiceTest.java
git commit -m "refactor: split member id generator port"
```

Expected: commit succeeds.

---

### Task 2: Add MySQL Testcontainers JPA Test Support

**Files:**
- Modify: `backend/build.gradle.kts`
- Create: `backend/src/test/java/com/dddblog/backend/support/MysqlDataJpaTestSupport.java`
- Modify: `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java`

- [ ] **Step 1: Update Gradle dependencies**

In `backend/build.gradle.kts`, replace this test dependency block portion:

```kotlin
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
```

with:

```kotlin
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mysql")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
```

- [ ] **Step 2: Add shared MySQL test support**

Create `backend/src/test/java/com/dddblog/backend/support/MysqlDataJpaTestSupport.java`:

```java
package com.dddblog.backend.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class MysqlDataJpaTestSupport {

	private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
		DockerImageName.parse("mysql:8.0.36")
	)
		.withDatabaseName("ddd_blog_test")
		.withUsername("test")
		.withPassword("test");

	static {
		MYSQL.start();
	}

	@DynamicPropertySource
	static void registerMysqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
		registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
	}
}
```

- [ ] **Step 3: Migrate the existing post persistence test**

Replace `backend/src/test/java/com/dddblog/backend/blog/persistence/JpaPostRepositoryAdapterTest.java` with:

```java
package com.dddblog.backend.blog.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.dddblog.backend.blog.domain.AuthorId;
import com.dddblog.backend.blog.domain.Post;
import com.dddblog.backend.blog.domain.PostContent;
import com.dddblog.backend.blog.domain.PostId;
import com.dddblog.backend.blog.domain.PostStatus;
import com.dddblog.backend.blog.domain.PostSummary;
import com.dddblog.backend.blog.domain.PostTitle;
import com.dddblog.backend.blog.domain.TagName;
import com.dddblog.backend.support.MysqlDataJpaTestSupport;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaPostRepositoryAdapter.class)
class JpaPostRepositoryAdapterTest extends MysqlDataJpaTestSupport {

	@Autowired
	private JpaPostRepositoryAdapter postRepository;

	@Autowired
	private SpringDataJpaPostRepository springDataPostRepository;

	@Autowired
	private SpringDataJpaTagRepository springDataTagRepository;

	@Autowired
	private TestEntityManager entityManager;

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

		entityManager.flush();
		entityManager.clear();

		JpaPostEntity savedPost = springDataPostRepository.findById(postId.value()).orElseThrow();
		assertThat(savedPost.authorId()).isEqualTo(1L);
		assertThat(savedPost.title()).isEqualTo("DDD 시작하기");
		assertThat(savedPost.contentMarkdown()).isEqualTo("# DDD\n\n본문");
		assertThat(savedPost.summary()).isEqualTo("DDD 소개");
		assertThat(savedPost.status()).isEqualTo(PostStatus.DRAFT);
		assertThat(savedPost.tags()).extracting(JpaTagEntity::name).containsExactlyInAnyOrder("ddd", "tdd");
	}

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

	@Test
	void 글이_null이면_저장할_수_없다() {
		assertThatThrownBy(() -> postRepository.save(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Post must not be null.");
		assertThat(springDataPostRepository.findAll()).isEmpty();
		assertThat(springDataTagRepository.findAll()).isEmpty();
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

- [ ] **Step 4: Run the migrated post persistence test**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.blog.persistence.JpaPostRepositoryAdapterTest
```

Expected: PASS. Docker must be running because this test starts a MySQL container.

- [ ] **Step 5: Commit**

Run from repository root:

```powershell
cd C:\dev\study\ddd-blog
git add backend\build.gradle.kts backend\src\test\java\com\dddblog\backend\support\MysqlDataJpaTestSupport.java backend\src\test\java\com\dddblog\backend\blog\persistence\JpaPostRepositoryAdapterTest.java
git commit -m "test: run jpa tests with mysql testcontainers"
```

Expected: commit succeeds.

---

### Task 3: Add Member Repository Adapter

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapterTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberEntity.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/persistence/SpringDataJpaMemberRepository.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapter.java`

- [ ] **Step 1: Write failing member persistence tests**

Create `backend/src/test/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapterTest.java`:

```java
package com.dddblog.backend.member.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLIntegrityConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.MemberRole;
import com.dddblog.backend.member.domain.MemberStatus;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;
import com.dddblog.backend.support.MysqlDataJpaTestSupport;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaMemberRepositoryAdapter.class)
class JpaMemberRepositoryAdapterTest extends MysqlDataJpaTestSupport {

	@Autowired
	private JpaMemberRepositoryAdapter memberRepository;

	@Autowired
	private SpringDataJpaMemberRepository springDataMemberRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 회원을_저장하면_ID를_반환한다() {
		Member member = createMember(1L, "user01", "길동");

		MemberId memberId = memberRepository.save(member);

		assertThat(memberId).isEqualTo(new MemberId(1L));
	}

	@Test
	void 회원을_저장하면_members에_도메인_값이_저장된다() {
		Member member = createMember(1L, "user01", "길동");

		memberRepository.save(member);

		entityManager.flush();
		entityManager.clear();

		JpaMemberEntity savedMember = springDataMemberRepository.findById(1L).orElseThrow();
		assertThat(savedMember.id()).isEqualTo(1L);
		assertThat(savedMember.name()).isEqualTo("홍길동");
		assertThat(savedMember.nickname()).isEqualTo("길동");
		assertThat(savedMember.loginId()).isEqualTo("user01");
		assertThat(savedMember.passwordHash()).isEqualTo("$2a$10$hashed-password");
		assertThat(savedMember.role()).isEqualTo(MemberRole.MEMBER);
		assertThat(savedMember.status()).isEqualTo(MemberStatus.ACTIVE);
	}

	@Test
	void 저장된_로그인_ID가_있으면_존재한다고_판단한다() {
		memberRepository.save(createMember(1L, "user01", "길동"));

		boolean exists = memberRepository.existsByLoginId(new LoginId("user01"));

		assertThat(exists).isTrue();
	}

	@Test
	void 저장된_닉네임이_있으면_존재한다고_판단한다() {
		memberRepository.save(createMember(1L, "user01", "길동"));

		boolean exists = memberRepository.existsByNickname(new Nickname("길동"));

		assertThat(exists).isTrue();
	}

	@Test
	void 로그인_ID는_중복_저장할_수_없다() {
		memberRepository.save(createMember(1L, "user01", "길동"));

		assertThatThrownBy(() -> {
			memberRepository.save(createMember(2L, "user01", "길동2"));
			entityManager.flush();
		})
			.hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
	}

	@Test
	void 닉네임은_중복_저장할_수_없다() {
		memberRepository.save(createMember(1L, "user01", "길동"));

		assertThatThrownBy(() -> {
			memberRepository.save(createMember(2L, "user02", "길동"));
			entityManager.flush();
		})
			.hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
	}

	@Test
	void 회원이_null이면_저장할_수_없다() {
		assertThatThrownBy(() -> memberRepository.save(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member must not be null.");
		assertThat(springDataMemberRepository.findAll()).isEmpty();
	}

	private Member createMember(Long id, String loginId, String nickname) {
		return Member.register(
			new MemberId(id),
			new MemberName("홍길동"),
			new Nickname(nickname),
			new LoginId(loginId),
			new PasswordHash("$2a$10$hashed-password")
		);
	}
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.member.persistence.JpaMemberRepositoryAdapterTest
```

Expected: FAIL because member persistence classes do not exist.

- [ ] **Step 3: Add member persistence classes**

Create `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberEntity.java`:

```java
package com.dddblog.backend.member.persistence;

import com.dddblog.backend.member.domain.MemberRole;
import com.dddblog.backend.member.domain.MemberStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "members",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = "login_id"),
		@UniqueConstraint(columnNames = "nickname")
	}
)
class JpaMemberEntity {

	@Id
	private Long id;

	@Column(nullable = false, length = 30)
	private String name;

	@Column(nullable = false, length = 20)
	private String nickname;

	@Column(name = "login_id", nullable = false, length = 30)
	private String loginId;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MemberRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MemberStatus status;

	protected JpaMemberEntity() {
	}

	JpaMemberEntity(
		Long id,
		String name,
		String nickname,
		String loginId,
		String passwordHash,
		MemberRole role,
		MemberStatus status
	) {
		this.id = id;
		this.name = name;
		this.nickname = nickname;
		this.loginId = loginId;
		this.passwordHash = passwordHash;
		this.role = role;
		this.status = status;
	}

	Long id() {
		return id;
	}

	String name() {
		return name;
	}

	String nickname() {
		return nickname;
	}

	String loginId() {
		return loginId;
	}

	String passwordHash() {
		return passwordHash;
	}

	MemberRole role() {
		return role;
	}

	MemberStatus status() {
		return status;
	}
}
```

Create `backend/src/main/java/com/dddblog/backend/member/persistence/SpringDataJpaMemberRepository.java`:

```java
package com.dddblog.backend.member.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJpaMemberRepository extends JpaRepository<JpaMemberEntity, Long> {

	boolean existsByLoginId(String loginId);

	boolean existsByNickname(String nickname);
}
```

Create `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapter.java`:

```java
package com.dddblog.backend.member.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;

@Repository
public class JpaMemberRepositoryAdapter implements MemberRepository {

	private final SpringDataJpaMemberRepository memberRepository;

	public JpaMemberRepositoryAdapter(SpringDataJpaMemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Override
	public boolean existsByLoginId(LoginId loginId) {
		return memberRepository.existsByLoginId(loginId.value());
	}

	@Override
	public boolean existsByNickname(Nickname nickname) {
		return memberRepository.existsByNickname(nickname.value());
	}

	@Override
	@Transactional
	public MemberId save(Member member) {
		if (member == null) {
			throw new IllegalArgumentException("Member must not be null.");
		}
		JpaMemberEntity entity = new JpaMemberEntity(
			member.id().value(),
			member.name().value(),
			member.nickname().value(),
			member.loginId().value(),
			member.passwordHash().value(),
			member.role(),
			member.status()
		);
		JpaMemberEntity savedEntity = memberRepository.save(entity);
		return new MemberId(savedEntity.id());
	}
}
```

- [ ] **Step 4: Run member persistence tests**

Run:

```powershell
.\gradlew.bat test --tests com.dddblog.backend.member.persistence.JpaMemberRepositoryAdapterTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run from repository root:

```powershell
cd C:\dev\study\ddd-blog
git add backend\src\test\java\com\dddblog\backend\member\persistence\JpaMemberRepositoryAdapterTest.java backend\src\main\java\com\dddblog\backend\member\persistence\JpaMemberEntity.java backend\src\main\java\com\dddblog\backend\member\persistence\SpringDataJpaMemberRepository.java backend\src\main\java\com\dddblog\backend\member\persistence\JpaMemberRepositoryAdapter.java
git commit -m "feat: add jpa member repository adapter"
```

Expected: commit succeeds.

---

### Task 4: Add DB-Backed Member ID Generator

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/member/persistence/JpaMemberIdGeneratorTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberIdSequenceEntity.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/persistence/SpringDataJpaMemberIdSequenceRepository.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberIdGenerator.java`

- [ ] **Step 1: Write failing ID generator tests**

Create `backend/src/test/java/com/dddblog/backend/member/persistence/JpaMemberIdGeneratorTest.java`:

```java
package com.dddblog.backend.member.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.support.MysqlDataJpaTestSupport;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaMemberIdGenerator.class)
class JpaMemberIdGeneratorTest extends MysqlDataJpaTestSupport {

	@Autowired
	private JpaMemberIdGenerator memberIdGenerator;

	@Test
	void ID를_발급하면_양수_ID를_반환한다() {
		MemberId memberId = memberIdGenerator.nextId();

		assertThat(memberId.value()).isPositive();
	}

	@Test
	void ID를_연속으로_발급하면_서로_다른_ID를_반환한다() {
		MemberId firstId = memberIdGenerator.nextId();
		MemberId secondId = memberIdGenerator.nextId();

		assertThat(firstId).isEqualTo(new MemberId(1L));
		assertThat(secondId).isEqualTo(new MemberId(2L));
	}
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.member.persistence.JpaMemberIdGeneratorTest
```

Expected: FAIL because ID generator persistence classes do not exist.

- [ ] **Step 3: Add ID sequence persistence classes**

Create `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberIdSequenceEntity.java`:

```java
package com.dddblog.backend.member.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "member_id_sequences")
class JpaMemberIdSequenceEntity {

	@Id
	@Column(length = 50)
	private String name;

	@Column(name = "next_value", nullable = false)
	private Long nextValue;

	protected JpaMemberIdSequenceEntity() {
	}

	JpaMemberIdSequenceEntity(String name, Long nextValue) {
		this.name = name;
		this.nextValue = nextValue;
	}

	String name() {
		return name;
	}

	Long nextValue() {
		return nextValue;
	}

	void increase() {
		nextValue++;
	}
}
```

Create `backend/src/main/java/com/dddblog/backend/member/persistence/SpringDataJpaMemberIdSequenceRepository.java`:

```java
package com.dddblog.backend.member.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface SpringDataJpaMemberIdSequenceRepository extends JpaRepository<JpaMemberIdSequenceEntity, String> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from JpaMemberIdSequenceEntity s where s.name = :name")
	Optional<JpaMemberIdSequenceEntity> findByNameWithLock(@Param("name") String name);
}
```

Create `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberIdGenerator.java`:

```java
package com.dddblog.backend.member.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dddblog.backend.member.application.MemberIdGenerator;
import com.dddblog.backend.member.domain.MemberId;

@Repository
public class JpaMemberIdGenerator implements MemberIdGenerator {

	private static final String MEMBER_SEQUENCE_NAME = "member";

	private final SpringDataJpaMemberIdSequenceRepository sequenceRepository;

	public JpaMemberIdGenerator(SpringDataJpaMemberIdSequenceRepository sequenceRepository) {
		this.sequenceRepository = sequenceRepository;
	}

	@Override
	@Transactional
	public MemberId nextId() {
		JpaMemberIdSequenceEntity sequence = sequenceRepository.findByNameWithLock(MEMBER_SEQUENCE_NAME)
			.orElseGet(() -> sequenceRepository.save(new JpaMemberIdSequenceEntity(MEMBER_SEQUENCE_NAME, 1L)));
		MemberId memberId = new MemberId(sequence.nextValue());
		sequence.increase();
		return memberId;
	}
}
```

- [ ] **Step 4: Run ID generator tests**

Run:

```powershell
.\gradlew.bat test --tests com.dddblog.backend.member.persistence.JpaMemberIdGeneratorTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run from repository root:

```powershell
cd C:\dev\study\ddd-blog
git add backend\src\test\java\com\dddblog\backend\member\persistence\JpaMemberIdGeneratorTest.java backend\src\main\java\com\dddblog\backend\member\persistence\JpaMemberIdSequenceEntity.java backend\src\main\java\com\dddblog\backend\member\persistence\SpringDataJpaMemberIdSequenceRepository.java backend\src\main\java\com\dddblog\backend\member\persistence\JpaMemberIdGenerator.java
git commit -m "feat: add jpa member id generator"
```

Expected: commit succeeds.

---

### Task 5: Full Verification

**Files:**
- Verify all files changed in Tasks 1-4.

- [ ] **Step 1: Run the full backend test suite**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 2: Check Korean test naming rule**

Run:

```powershell
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected: no output.

- [ ] **Step 3: Check pure domain/application packages**

Run:

```powershell
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional'
```

Expected: no output.

- [ ] **Step 4: Confirm H2 dependency was removed**

Run:

```powershell
Select-String -Path .\build.gradle.kts -Pattern 'com.h2database:h2'
```

Expected: no output.

- [ ] **Step 5: Confirm working tree status**

Run from repository root:

```powershell
cd C:\dev\study\ddd-blog
git status --short
```

Expected: no uncommitted files after Task 4 commit.

---

## Self-Review

- Spec coverage: Task 1 implements `MemberIdGenerator`, removes `MemberRepository.nextId()`, and updates `RegisterMemberService`. Task 2 adds Testcontainers MySQL infrastructure, removes H2, and migrates the existing post persistence test. Task 3 adds member persistence adapter, `members` mapping, duplicate lookup methods, unique constraints, and null guard. Task 4 adds DB table-backed member ID generation with pessimistic locking. Task 5 covers all verification commands.
- Scope check: The plan does not add REST API, BCrypt, JWT, Flyway/Liquibase, member read/update/delete methods, Spring configuration for `RegisterMemberService`, or a `posts.author_id` FK.
- Type consistency: `MemberIdGenerator.nextId()` returns `MemberId`; `MemberRepository` contains only `existsByLoginId`, `existsByNickname`, and `save`; `RegisterMemberService` receives both `MemberRepository` and `MemberIdGenerator`.
- Test naming check: All test methods in the plan are Korean scenario-style names joined with `_`.
- Persistence boundary check: Spring/JPA annotations are introduced only under `member.persistence` and existing `blog.persistence` tests, not in `member.domain` or `member.application`.
