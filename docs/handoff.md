# DDD Blog Handoff

## Current State

- Main workspace: `C:\dev\study\ddd-blog`
- Active implementation worktree: none
- Backend: `C:\dev\study\ddd-blog\backend`
- Java for this project: `C:\java\jdk-21`
- Spring Boot: `3.5.0`
- Current branch: `master`
- Latest completed backend slice: login and JWT authentication.
- Latest completed commit: `b50892b fix: correct current member handoff behavior`
- Recent implementation commits are listed in Recent Commits.

## Project Rules

See `AGENTS.md`.

Important local rules:

- Backend test method names must be Korean scenario-style names.
- Korean test method words are joined with `_`.
- Pure domain/application tests must not start Spring Context.
- `member.domain` and `member.application` must remain free of Spring/JPA annotations.
- Domain rules should stay in value objects and aggregates where possible.
- Spring/JPA annotations are allowed in adapter, API, auth security, and config packages when explicitly designed.
- Prefer TDD: write a small failing test first, then implement the minimum code to pass.
- For new feature slices, use `superpowers:brainstorming`, write a Korean spec, then use `superpowers:writing-plans`.
- There is no active feature worktree after the login/JWT branch was merged locally into `master`.

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
- `docs/superpowers/specs/2026-06-17-login-jwt-auth-design.md`
- `docs/superpowers/plans/2026-06-17-login-jwt-auth.md`

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
- `MemberRepository.findByLoginId(loginId)` reads members for login.
- `MemberRepository.findById(memberId)` reads members for the current-member API.

`MemberRepository` methods:

```java
boolean existsByLoginId(LoginId loginId);
boolean existsByNickname(Nickname nickname);
Optional<Member> findByLoginId(LoginId loginId);
Optional<Member> findById(MemberId memberId);
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

### Signup API Behavior

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
- Other requests are authenticated by default, except `POST /api/auth/login`.
- `httpBasic`, `formLogin`, and server-side `logout` are disabled.

### Implemented Auth Behavior

- `POST /api/auth/login` authenticates by login ID and password.
- Successful login returns `{ "accessToken": "..." }`.
- Access Token is a JWT containing member ID, role, token type, issued-at, and expiration.
- `Authorization: Bearer <token>` authenticates protected requests.
- `GET /api/members/me` returns member ID, name, nickname, login ID, and role.
- Login failure, missing token, invalid token, expired token, and inactive member all return `401 { "message": "Authentication failed." }`.
- Logout is client-side token deletion. There is no server logout endpoint in this slice.

Auth packages:

```text
backend/src/main/java/com/dddblog/backend/auth/api
backend/src/main/java/com/dddblog/backend/auth/application
backend/src/main/java/com/dddblog/backend/auth/security
```

Implemented:

- `LoginController`
- `LoginRequest`
- `LoginResponse`
- `LoginService`
- `AuthenticationFailedException`
- `AccessTokenIssuer`
- `JwtTokenProvider`
- `JwtProperties`
- `JwtAuthenticationFilter`
- `JwtAuthenticationEntryPoint`
- `JwtAuthentication`
- `AuthenticatedMember`
- `ParsedAccessToken`

Important auth behavior:

- `LoginService.login(loginId, password)` reads the member by `LoginId`.
- Invalid login ID format is normalized to `Authentication failed.`
- Missing password is normalized to `Authentication failed.`
- Password checks use Spring Security's `PasswordEncoder`.
- Inactive members cannot log in.
- `JwtTokenProvider` signs access tokens with the configured JWT secret.
- JWT subject stores `memberId`.
- JWT claims include `role` and token `type`.
- Only token type `access` is accepted by `parseAccessToken`.
- JWT parse, signature, expiration, malformed-token, and wrong-type failures are normalized to `Authentication failed.`
- `JwtAuthenticationFilter` reads only `Authorization: Bearer ...` tokens.
- Missing or non-Bearer authorization headers continue through the filter chain and protected endpoints fail through Spring Security's authentication entry point.
- Invalid Bearer tokens clear the security context and return the authentication entry point response.
- `MeController` uses `@AuthenticationPrincipal AuthenticatedMember`.
- `MeService` reloads the current member by ID; a missing member returns `Authentication failed.`

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

Important blog persistence exclusions still true:

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
- `findByLoginId(loginId)` maps a persisted row back to a `Member`.
- `findById(memberId)` maps a persisted row back to a `Member`.
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
- No member update/delete repository methods yet.
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
- `GlobalExceptionHandler` maps `AuthenticationFailedException` to `401 Unauthorized`.
- Error body shape is `{ "message": "..." }`.
- `PasswordConfig` provides a `BCryptPasswordEncoder`.
- `MemberApplicationConfig` wires `RegisterMemberService` from pure application ports.
- `SecurityConfig` permits unauthenticated signup and login.
- `SecurityConfig` authenticates all other requests by default.
- `SecurityConfig` is stateless and disables CSRF, HTTP Basic, form login, and server-side logout.
- `SecurityConfig` adds `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.

### Dependencies

Backend Gradle dependencies include:

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `spring-boot-starter-web`
- `mysql-connector-j`
- `io.jsonwebtoken:jjwt-api`
- `io.jsonwebtoken:jjwt-impl` as `runtimeOnly`
- `io.jsonwebtoken:jjwt-jackson` as `runtimeOnly`
- `org.testcontainers:junit-jupiter` as `testImplementation`
- `org.testcontainers:mysql` as `testImplementation`

H2 was removed from test runtime dependencies during the Testcontainers migration and was not reintroduced.

## Exclusions Still True

- No Refresh Token.
- No token reissue.
- No server-side logout or blacklist.
- No member update/delete API.
- No post API integration with authenticated member yet.

Additional exclusions still true:

- No Flyway/Liquibase migration yet.
- No relation/FK between `posts.author_id` and `members.id` yet.
- No post read/update/delete API yet.

## Tests

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

Auth application tests:

```text
backend/src/test/java/com/dddblog/backend/auth/application
```

Auth API tests:

```text
backend/src/test/java/com/dddblog/backend/auth/api
```

Auth security tests:

```text
backend/src/test/java/com/dddblog/backend/auth/security
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

Login/auth test scenarios include:

- Login service success with valid login ID and password.
- Login service failure for missing member, invalid login ID format, wrong password, missing password, and inactive member.
- Login controller success response with `accessToken`.
- Login controller `401` response with `Authentication failed.`
- JWT token provider creates and parses access tokens.
- JWT token provider rejects malformed, invalid, expired, or wrong-type tokens with normalized failure.
- JWT authentication filter sets authentication for valid Bearer tokens.
- JWT authentication filter clears authentication and returns the entry point for invalid Bearer tokens.
- Current-member API returns member ID, name, nickname, login ID, and role.
- Current-member API returns `401` for missing token, invalid Bearer token, non-member authentication principal, or missing authenticated member.
- Vertical integration verifies signup, login, Bearer-token authentication, and `/api/members/me`.

Important signup integration test detail:

- The vertical integration test posts to `/api/auth/signup`.
- It asserts `201 Created`.
- It asserts the response `memberId` is the same ID persisted in `members`.
- It asserts `members.password_hash` is not the raw password.
- It verifies the stored hash matches the raw password with `BCryptPasswordEncoder`.

Important auth integration test detail:

- The vertical integration test signs up a member, then posts to `/api/auth/login`.
- It asserts `200 OK`.
- It asserts the response contains a string `accessToken`.
- It calls `GET /api/members/me` with `Authorization: Bearer <accessToken>`.
- It asserts the response contains member ID, name, nickname, login ID, and role.
- It verifies invalid Bearer tokens return `401 { "message": "Authentication failed." }`.

Important persistence test detail:

- JPA tests use MySQL Testcontainers through `MysqlDataJpaTestSupport`.
- Docker must be running for persistence and vertical integration tests.
- Persisted value tests use `TestEntityManager.flush()` and `clear()` before reloading saved rows where applicable.
- Full test runs currently pass, but Hibernate may log non-blocking schema-drop noise during shutdown because multiple JPA slice contexts share the static MySQL test container with `ddl-auto=create-drop`.

All backend test method names are Korean scenario-style.

## Verification Commands

Run from backend:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

Check test naming rule:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected: no output.

Check no Spring/JPA annotations in pure domain/application packages:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional|@Configuration|@Bean'
```

Expected: no output.

Check H2 was not reintroduced:

```powershell
cd C:\dev\study\ddd-blog\backend
rg -n "h2database|jdbc:h2|H2Dialect|com\.h2database" .
```

Expected: no matches. `rg` exits with code `1` when there are no matches.

Check whitespace:

```powershell
cd C:\dev\study\ddd-blog
git diff --check
```

Expected: no output.

## Recent Commits

Most relevant login/JWT implementation commits now merged into `master`:

```text
c524850 test: verify login jwt auth flow
cd65aad test: localize junit lifecycle method names
d0d14b2 docs: update handoff after login jwt auth
ded5ef2 fix: correct login jwt handoff paths
b50892b fix: correct current member handoff behavior
85ef2e4 fix: tighten current member api tests
0db36c4 feat: add current member api
c5d1b07 fix: cover jwt authentication filter behavior
c934338 feat: add jwt authentication filter
3cf9a13 fix: wire authentication entry point
f5d0127 feat: add login api
a7364df fix: normalize missing login password failure
a0b8a7f feat: add login service
69f9a3f feat: add member repository read methods
208b5c9 fix: avoid leaking jwt parse causes
db1cfbc feat: add jwt token provider
dd0783c build: add jwt dependencies
33d8164 docs: add login jwt auth implementation plan
c5dcbab docs: add login jwt auth design
```

Earlier signup API commits remain relevant background:

```text
53fe760 test: tie signup response id to persisted member
6a19bde test: verify signup api vertical slice
508f65e feat: add signup controller
def93c3 fix: avoid basic auth in signup security config
1626a77 feat: configure signup dependencies
23b20a3 feat: add signup api facade service
0bc1dd5 feat: add raw password value object
e8891eb docs: add signup api implementation plan
ad51175 docs: add signup api design
```

## Recommended Next Step

Recommended next milestone: authenticated post creation API.

```text
POST /api/posts
```

Why this is next:

- The project requirements sequence after signup/login/JWT/current-member lookup is authenticated post creation.
- Blog domain, `CreatePostService`, and `JpaPostRepositoryAdapter` already exist.
- The new auth slice provides `AuthenticatedMember.memberId`, which can be converted to `AuthorId` for post creation.
- This milestone will connect existing Blog application/persistence behavior to HTTP while exercising authorization identity flow.

Suggested scope:

- Brainstorm and write a spec for `POST /api/posts`.
- Add a Spring configuration that exposes pure `CreatePostService` as a bean.
- Add a thin API layer that accepts title, content Markdown, summary, tags, and status.
- Derive author ID from `AuthenticatedMember`, not from request body.
- Return `201 Created` with created post ID.
- Keep image/cover image, post read APIs, update/delete APIs, publish workflows, and post/member FK out of this slice unless explicitly approved.

Do not start without a new task:

- Refresh Token, token reissue, server-side logout/blacklist, member update/delete APIs, Flyway/Liquibase, post/member FK, post read/update/delete APIs, or frontend work.

## Notes For Next Agent

- Start by reading this handoff.
- Then read `docs/requirements.md`, `docs/superpowers/specs/2026-06-07-create-post-application-design.md`, and `docs/superpowers/plans/2026-06-07-create-post-application.md`.
- Start a new feature slice from `master`.
- Use `superpowers:brainstorming` before designing the authenticated post creation API.
- Login/JWT auth slice implementation is merged into `master` as of commit `b50892b`.
- Use `superpowers:finishing-a-development-branch` only when explicitly asked to finish the branch.
- The user prefers Korean documentation and Korean test method names.
- Preserve pure `member.domain` and `member.application` style.
- Do not add unrelated refactors or cleanup.
- Current `master` includes the login/JWT implementation commits.
- Be careful with helper method names under `backend/src/test/java`: the naming-rule command flags ASCII `void` methods in all test source files, including fakes.
- Be careful not to reintroduce DB-generated member IDs. `members.id` is assigned from `Member.id()`.
- Be careful not to use Spring Data `save(entity)` for create-only member persistence with assigned IDs; `EntityManager.persist(entity)` is intentional.
- Known non-blocking issue: full test runs may print Hibernate schema-drop noise from shared MySQL Testcontainers + `ddl-auto=create-drop`, but the Gradle test result should still be successful.
