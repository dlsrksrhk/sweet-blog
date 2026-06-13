# DDD Blog Handoff

## Current State

- Main workspace: `C:\dev\study\ddd-blog`
- Active implementation worktree: `C:\dev\study\ddd-blog\.worktrees\signup-api`
- Backend: `C:\dev\study\ddd-blog\.worktrees\signup-api\backend`
- Java for this project: `C:\java\jdk-21`
- Spring Boot: `3.5.0`
- Current implementation branch: `codex/signup-api`
- Signup API implementation plan Tasks 1-5 are complete.
- Task 6 final verification is complete after this handoff refresh.
- Latest handoff commit: `517c687 docs: include error response in signup handoff`
- Latest code commit before the handoff updates: `53fe760 test: tie signup response id to persisted member`
- Latest plan/design commits:
  - `e8891eb docs: add signup api implementation plan`
  - `ad51175 docs: add signup api design`

## Project Rules

See `AGENTS.md`.

Important local rules:

- Backend test method names must be Korean scenario-style names.
- Korean test method words are joined with `_`.
- Pure domain/application tests must not start Spring Context.
- Domain/application classes in current learning slices should not use Spring or JPA annotations.
- Spring/JPA annotations are allowed in adapter, API, and config packages when explicitly designed.
- Prefer TDD: write a small failing test first, then implement the minimum code to pass.
- For new feature slices, use `superpowers:brainstorming`, write a Korean spec, then use `superpowers:writing-plans`.
- Current slice already has approved spec and plan. Do not restart brainstorming for this branch.
- Current implementation is already in an isolated worktree.

## Implemented So Far

### Requirements And Planning

- `docs/requirements.md`
  - General DDD/TDD blog requirements.
  - Comment requirements added.
  - Comments removed from excluded scope.
  - Comment extension task clarified as `댓글 기능 고도화`.
- `docs/superpowers/specs/2026-06-07-post-domain-design.md`
- `docs/superpowers/plans/2026-06-07-post-domain.md`
- `docs/superpowers/specs/2026-06-07-create-post-application-design.md`
- `docs/superpowers/plans/2026-06-07-create-post-application.md`
- `docs/superpowers/specs/2026-06-07-jpa-post-repository-design.md`
- `docs/superpowers/plans/2026-06-07-jpa-post-repository.md`
- `docs/superpowers/specs/2026-06-07-member-domain-design.md`
- `docs/superpowers/plans/2026-06-07-member-domain.md`
- `docs/superpowers/specs/2026-06-08-register-member-application-design.md`
- `docs/superpowers/plans/2026-06-08-register-member-application.md`
- `docs/superpowers/specs/2026-06-09-member-persistence-id-generation-design.md`
- `docs/superpowers/plans/2026-06-09-member-persistence-id-generation.md`
- `docs/superpowers/specs/2026-06-13-signup-api-design.md`
- `docs/superpowers/plans/2026-06-13-signup-api.md`

### Blog Domain Layer

Package:

```text
backend/src/main/java/com/dddblog/backend/blog/domain
```

Implemented:

- `AuthorId`
- `Post`
- `PostContent`
- `PostId`
- `PostStatus`
- `PostSummary`
- `PostTitle`
- `TagName`

Important blog domain behavior:

- `Post` has no persistence ID field yet.
- `PostId` is used as repository save result for now.
- `Post` validates author/title/content/status.
- `Post` normalizes null summary through `PostSummary`.
- `Post` treats null tag list as empty.
- `Post` rejects null tag elements.
- `Post` rejects more than 10 tags.
- `Post` rejects duplicate normalized tags.
- `TagName` trims and lowercases with `Locale.ROOT`.
- `AuthorId` is still a Blog-context value object used to reference the author/member ID.

Blog domain package is still pure Java:

- No Spring annotations.
- No JPA annotations.

### Member Domain Layer

Package:

```text
backend/src/main/java/com/dddblog/backend/member/domain
```

Implemented:

- `LoginId`
- `Member`
- `MemberId`
- `MemberName`
- `MemberRole`
- `MemberStatus`
- `Nickname`
- `PasswordHash`
- `RawPassword`

Important member domain behavior:

- `Member.register(...)` creates a new member with:
  - `MemberRole.MEMBER`
  - `MemberStatus.ACTIVE`
- `Member` validates all constructor dependencies are non-null.
- `MemberId` is Long-backed and must be non-null and positive.
- `MemberName` trims input and allows 1 to 30 characters.
- `Nickname` trims input and allows 2 to 20 characters.
- `LoginId` trims input and allows 4 to 30 characters.
- `PasswordHash` rejects null/blank and stores the given hash string unchanged.
- `PasswordHash.value()` returns the stored hash.
- `PasswordHash.toString()` returns `[PROTECTED]` to avoid accidental credential-material leakage.
- `RawPassword` rejects null, blank, and values shorter than 8 characters.
- `RawPassword.value()` returns the original raw password value for hashing.
- `MemberRole` values are `MEMBER`, `ADMIN`.
- `MemberStatus` values are `ACTIVE`, `INACTIVE`.

Important exclusions still true:

- No login/JWT integration yet.
- No member read/update/delete API yet.
- No persistence-backed authentication flow yet.
- No relation/FK between `posts.author_id` and a member table yet.

Member domain package is pure Java:

- No Spring annotations.
- No JPA annotations.

### Member Application Layer

Package:

```text
backend/src/main/java/com/dddblog/backend/member/application
```

Implemented:

- `RegisterMemberCommand`
- `RegisterMemberService`
- `MemberRepository`
- `MemberIdGenerator`

Behavior:

- `RegisterMemberCommand` fields are:
  - `String name`
  - `String nickname`
  - `String loginId`
  - `String passwordHash`
- `RegisterMemberCommand` does not receive `memberId`.
- `RegisterMemberService.register(command)` rejects null command with `Register member command must not be null.`
- `RegisterMemberService.register(command)` converts command primitives to:
  - `MemberName`
  - `Nickname`
  - `LoginId`
  - `PasswordHash`
- Value-object exceptions are not caught or translated.
- Duplicate login ID is checked through `MemberRepository.existsByLoginId(loginId)`.
- Duplicate nickname is checked through `MemberRepository.existsByNickname(nickname)`.
- Duplicate login ID is rejected with `Login id already exists.`
- Duplicate nickname is rejected with `Nickname already exists.`
- `MemberIdGenerator.nextId()` is called only after command validation and duplicate checks pass.
- `Member.register(memberId, name, nickname, loginId, passwordHash)` creates the new member.
- `MemberRepository.save(member)` is called and its returned `MemberId` is returned from the service.

`MemberRepository` methods:

```java
boolean existsByLoginId(LoginId loginId);
boolean existsByNickname(Nickname nickname);
MemberId save(Member member);
```

`MemberIdGenerator` methods:

```java
MemberId nextId();
```

Important wiring note:

- `RegisterMemberService` is a pure Java class.
- It is not annotated with `@Service`.
- `backend/src/main/java/com/dddblog/backend/member/config/MemberApplicationConfig.java` exposes it as a Spring bean for the API layer.

Member application package is pure Java:

- No Spring annotations.
- No JPA annotations.

### Member API Layer

Package:

```text
backend/src/main/java/com/dddblog/backend/member/api
```

Implemented:

- `SignupController`
- `SignupRequest`
- `SignupResponse`
- `SignupService`

Behavior:

- `POST /api/auth/signup` accepts `name`, `nickname`, `loginId`, and raw `password`.
- `SignupService` validates the raw password with `RawPassword`.
- `SignupService` hashes the password with BCrypt before calling `RegisterMemberService`.
- Successful signup returns `201 Created` with `memberId`.
- `IllegalArgumentException` is returned as `400 Bad Request` with `{ "message": "..." }`.

Security behavior:

- `POST /api/auth/signup` is unauthenticated through `SecurityConfig`.
- Other requests remain authenticated by default.
- `httpBasic` and `formLogin` were not enabled for this slice.
- No login/JWT/read API work is included in this slice.

### Blog Application Layer

Package:

```text
backend/src/main/java/com/dddblog/backend/blog/application
```

Implemented:

- `CreatePostCommand`
- `CreatePostService`
- `PostRepository`

Behavior:

- `CreatePostService.create(command)` converts command primitives to domain value objects.
- It creates a `Post`.
- It calls `PostRepository.save(post)`.
- It returns `PostId`.
- Null command is rejected with `Create post command must not be null.`
- Domain exceptions are not caught or translated.

Important wiring note:

- `CreatePostService` is still a pure Java class.
- It is not annotated with `@Service`.
- There is not yet a Spring configuration that exposes `CreatePostService` as a bean.
- A future API slice must decide how to wire it.

### Blog Persistence Layer

Package:

```text
backend/src/main/java/com/dddblog/backend/blog/persistence
```

Implemented:

- `JpaPostEntity`
- `JpaTagEntity`
- `SpringDataJpaPostRepository`
- `SpringDataJpaTagRepository`
- `JpaPostRepositoryAdapter`

Behavior:

- `JpaPostRepositoryAdapter` implements application port `PostRepository`.
- `JpaPostRepositoryAdapter.save(post)` rejects null post with `Post must not be null.`
- `save(post)` is annotated with `@Transactional`.
- `save(post)` maps domain `Post` to `JpaPostEntity`.
- It persists tags through normalized tag names.
- Existing tags are reused by `SpringDataJpaTagRepository.findByName(name)`.
- New tags are created with `new JpaTagEntity(tagName.value())`.
- It saves the post and returns `new PostId(savedEntity.id())`.

Table mapping:

- `posts`
  - `id`
  - `author_id`
  - `title`
  - `content_markdown`
  - `summary`
  - `status`
- `tags`
  - `id`
  - `name`
  - `name` has unique constraint.
- `post_tags`
  - `post_id`
  - `tag_id`
  - `(post_id, tag_id)` has unique constraint.

Important exclusions still true:

- No read repository method yet.
- No update/delete repository method yet.
- No auditing columns yet.
- No soft delete/view count/published at/cover image persistence yet.
- No Flyway/Liquibase yet.
- Repository tests use MySQL Testcontainers instead of H2.

### Member Persistence Layer

Package:

```text
backend/src/main/java/com/dddblog/backend/member/persistence
```

Implemented:

- `JpaMemberEntity`
- `SpringDataJpaMemberRepository`
- `JpaMemberRepositoryAdapter`
- `JpaMemberIdSequenceEntity`
- `SpringDataJpaMemberIdSequenceRepository`
- `JpaMemberIdGenerator`

Behavior:

- `JpaMemberRepositoryAdapter` implements application port `MemberRepository`.
- `JpaMemberRepositoryAdapter.save(member)` rejects null member with `Member must not be null.`
- `save(member)` is annotated with `@Transactional`.
- `save(member)` maps domain `Member` to `JpaMemberEntity`.
- Member IDs are assigned by the domain/application flow before persistence.
- `JpaMemberEntity.id` is not DB-generated; it stores `member.id().value()`.
- `save(member)` uses `EntityManager.persist(entity)` rather than Spring Data `save(entity)` so duplicate assigned IDs fail instead of merging/updating.
- `save(member)` returns `new MemberId(entity.id())`.
- Duplicate login ID is checked through `SpringDataJpaMemberRepository.existsByLoginId(loginId)`.
- Duplicate nickname is checked through `SpringDataJpaMemberRepository.existsByNickname(nickname)`.
- `JpaMemberIdGenerator` uses table `member_id_sequences`.
- The `member` sequence row is initialized before first use.
- ID generation uses a pessimistic write lock for increments.

Table mapping:

- `members`
  - `id`
  - `name`
  - `nickname`
  - `login_id`
  - `password_hash`
  - `role`
  - `status`
  - `nickname` has unique constraint.
  - `login_id` has unique constraint.
- `member_id_sequences`
  - `name`
  - `next_value`

Important member persistence details:

- `members.id` is intentionally assigned, not auto-increment.
- No member read/update/delete repository methods yet.
- No Flyway/Liquibase migration yet.
- No relation/FK between `posts.author_id` and `members.id` yet.

### Common API And Config

Implemented:

- `backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java`
- `backend/src/main/java/com/dddblog/backend/common/api/ErrorResponse.java`
- `backend/src/main/java/com/dddblog/backend/config/PasswordConfig.java`
- `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`
- `backend/src/main/java/com/dddblog/backend/member/config/MemberApplicationConfig.java`

Behavior:

- `GlobalExceptionHandler` maps `IllegalArgumentException` to `400 Bad Request`.
- Error body shape is `{ "message": "..." }`.
- `PasswordConfig` provides a `BCryptPasswordEncoder`.
- `MemberApplicationConfig` wires `RegisterMemberService` from pure application ports.
- `SecurityConfig` permits unauthenticated signup and authenticates other requests.

### Dependencies

Backend Gradle dependencies include:

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `spring-boot-starter-web`
- `mysql-connector-j`
- `org.testcontainers:junit-jupiter` as `testImplementation`
- `org.testcontainers:mysql` as `testImplementation`

H2 was removed from test runtime dependencies during the Testcontainers migration and was not reintroduced.

### Tests

Blog domain tests:

```text
backend/src/test/java/com/dddblog/backend/blog/domain
```

Member domain tests:

```text
backend/src/test/java/com/dddblog/backend/member/domain
```

Member application tests:

```text
backend/src/test/java/com/dddblog/backend/member/application
```

Member API tests:

```text
backend/src/test/java/com/dddblog/backend/member/api
```

Blog application tests:

```text
backend/src/test/java/com/dddblog/backend/blog/application
```

Blog persistence tests:

```text
backend/src/test/java/com/dddblog/backend/blog/persistence
```

Member persistence tests:

```text
backend/src/test/java/com/dddblog/backend/member/persistence
```

JPA test support:

```text
backend/src/test/java/com/dddblog/backend/support/MysqlDataJpaTestSupport.java
```

Implemented application test fakes:

- `FakePostRepository`
- `FakeMemberRepository`
- `FakeMemberIdGenerator`

Raw password test scenarios:

- `비밀번호가_null이면_생성할_수_없다`
- `비밀번호가_blank이면_생성할_수_없다`
- `비밀번호가_8자_미만이면_생성할_수_없다`
- `유효한_비밀번호이면_원문_값을_반환한다`

Signup service test scenarios:

- `회원가입을_요청하면_비밀번호를_해시해서_회원가입_서비스에_전달한다`
- `회원가입에_성공하면_회원_ID를_반환한다`
- `비밀번호가_8자_미만이면_회원가입_서비스를_호출하지_않는다`

Signup controller test scenarios:

- `회원가입에_성공하면_201과_회원_ID를_반환한다`
- `회원가입_요청이_실패하면_400과_오류_메시지를_반환한다`
- `인증_없이_회원가입을_요청할_수_있다`

Signup integration test scenario:

- `회원가입_API는_BCrypt로_해시한_비밀번호를_members에_저장한다`

Important signup integration test detail:

- The vertical integration test posts to `/api/auth/signup`.
- It asserts `201 Created`.
- It asserts the response `memberId` is the same ID persisted in `members`.
- It asserts `members.password_hash` is not the raw password.
- It verifies the stored hash matches the raw password with `BCryptPasswordEncoder`.

Important persistence test detail:

- JPA tests use MySQL Testcontainers through `MysqlDataJpaTestSupport`.
- Docker must be running for persistence and vertical integration tests.
- Persisted value tests use `TestEntityManager.flush()` and `clear()` before reloading saved rows where applicable.
- Full test runs currently pass, but Hibernate may log non-blocking schema-drop noise during shutdown because multiple JPA slice contexts share the static MySQL test container with `ddl-auto=create-drop`.

All backend test method names are Korean scenario-style.

## Verification Commands

Run from backend:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\signup-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

Check test naming rule:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\signup-api\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected: no output.

Check no Spring/JPA annotations in pure domain/application packages:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\signup-api\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional|@Configuration|@Bean'
```

Expected: no output.

Check H2 was not reintroduced:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\signup-api\backend
rg -n "h2database|jdbc:h2|H2Dialect|com\.h2database" .
```

Expected: no matches. `rg` exits with code `1` when there are no matches.

Task 6 verification results on 2026-06-13:

- `.\gradlew.bat test --rerun-tasks`: exit `0`, `BUILD SUCCESSFUL`.
- Test method naming scan: exit `0`, no output.
- Pure package annotation scan: exit `0`, no output.
- H2 scan: exit `1`, no matches.

## Recent Commits

Most relevant recent commits on `codex/signup-api`:

```text
4fc122d docs: update handoff after signup api
53fe760 test: tie signup response id to persisted member
6a19bde test: verify signup api vertical slice
508f65e feat: add signup controller
def93c3 fix: avoid basic auth in signup security config
1626a77 feat: configure signup dependencies
23b20a3 feat: add signup api facade service
0bc1dd5 feat: add raw password value object
e8891eb docs: add signup api implementation plan
ad51175 docs: add signup api design
22a3910 docs: refresh handoff after final verification
cd7dd9d fix: initialize member id sequence before use
2d92cc2 fix: align mysql test support helper naming
```

## Recommended Next Step

Finish the development branch after final review:

```text
docs/superpowers/plans/2026-06-13-signup-api.md
```

Completed signup API slice files:

- `backend/src/main/java/com/dddblog/backend/member/domain/RawPassword.java`
- `backend/src/main/java/com/dddblog/backend/member/api/SignupController.java`
- `backend/src/main/java/com/dddblog/backend/member/api/SignupRequest.java`
- `backend/src/main/java/com/dddblog/backend/member/api/SignupResponse.java`
- `backend/src/main/java/com/dddblog/backend/member/api/SignupService.java`
- `backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java`
- `backend/src/main/java/com/dddblog/backend/common/api/ErrorResponse.java`
- `backend/src/main/java/com/dddblog/backend/config/PasswordConfig.java`
- `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`
- `backend/src/main/java/com/dddblog/backend/member/config/MemberApplicationConfig.java`
- `backend/src/test/java/com/dddblog/backend/member/domain/RawPasswordTest.java`
- `backend/src/test/java/com/dddblog/backend/member/api/SignupServiceTest.java`
- `backend/src/test/java/com/dddblog/backend/member/api/SignupControllerTest.java`
- `backend/src/test/java/com/dddblog/backend/member/api/SignupApiIntegrationTest.java`

Do not start without a new task:

- Login/JWT, member read/update/delete APIs, Flyway/Liquibase, or Post/member FK.

## Notes For Next Agent

- The user wants the controller to handle final branch finishing after this review.
- Start by reading this handoff.
- Then read `docs/superpowers/plans/2026-06-13-signup-api.md`.
- Continue in worktree `C:\dev\study\ddd-blog\.worktrees\signup-api` on branch `codex/signup-api`.
- Signup API Tasks 1, 2, 3, 4, and 5 are complete.
- Task 6 verification passed locally on 2026-06-13.
- Use `superpowers:finishing-a-development-branch` only when explicitly asked to finish the branch.
- The user prefers Korean documentation and Korean test method names.
- Preserve pure domain/application style.
- Do not add unrelated refactors or cleanup.
- Current `master` does not include this branch's implementation commits yet.
- Be careful with helper method names under `backend/src/test/java`: the naming-rule command flags ASCII `void` methods in all test source files, including fakes.
- Be careful not to reintroduce DB-generated member IDs. `members.id` is assigned from `Member.id()`.
- Be careful not to use Spring Data `save(entity)` for create-only member persistence with assigned IDs; `EntityManager.persist(entity)` is intentional.
- Known non-blocking issue: full test runs may print Hibernate schema-drop noise from shared MySQL Testcontainers + `ddl-auto=create-drop`, but the Gradle test result should still be successful.
