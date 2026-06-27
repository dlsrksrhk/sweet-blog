# DDD Blog Handoff

## Current State

- Main workspace: `C:\dev\study\ddd-blog`
- Active implementation worktree: none
- Backend: `C:\dev\study\ddd-blog\backend`
- Java for this project: `C:\java\jdk-21`
- Spring Boot: `3.5.0`
- Current branch: `master`
- Latest completed backend slice: public post detail read API.
- Latest completed backend slice commit: `ab62990 test: localize post detail query fake helper`
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
- There is no active feature worktree after the public post detail read API branch was merged locally into `master` and pushed to `origin/master`.

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
- `docs/superpowers/specs/2026-06-27-authenticated-post-create-api-design.md`
- `docs/superpowers/plans/2026-06-27-authenticated-post-create-api.md`
- `docs/superpowers/specs/2026-06-27-post-detail-read-api-design.md`
- `docs/superpowers/plans/2026-06-27-post-detail-read-api.md`

### Blog Domain Layer

Package:

```text
backend/src/main/java/com/dddblog/backend/blog/domain
```

Implemented:

- `AuthorId`
- `Post`
- `PostContent`
- `PostContentType`
- `PostId`
- `PostStatus`
- `PostSummary`
- `PostTitle`
- `TagName`

Important blog domain behavior:

- `Post` has no persistence ID field yet.
- `PostId` is used as repository save result for now.
- `Post` validates author/title/content/status.
- `Post` validates content type.
- `PostContentType` values are `MARKDOWN`, `HTML`.
- `PostContentType.HTML` is represented in the domain for future expansion, but the current HTTP API rejects it.
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
- `PostDetail`
- `PostDetailQueryRepository`
- `PostDetailQueryService`
- `PostNotFoundException`
- `PostRepository`

Behavior:

- `CreatePostService.create(command)` converts command primitives to domain value objects.
- It creates a `Post`.
- It calls `PostRepository.save(post)`.
- It returns `PostId`.
- Null command is rejected with `Create post command must not be null.`
- Domain exceptions are not caught or translated.
- `CreatePostCommand` fields are:
  - `Long authorId`
  - `String title`
  - `PostContentType contentType`
  - `String content`
  - `String summary`
  - `List<String> tags`
  - `PostStatus status`
- `PostDetailQueryService.getDetail(postId)` returns public post detail read data.
- `PostDetailQueryService.getDetail(postId)` rejects null post ID with `Post id must not be null.`
- `PostDetailQueryService.getDetail(postId)` throws `PostNotFoundException` with `Post not found.` when no public result exists.
- `PostDetailQueryRepository.findPublishedById(postId)` is a read port separate from the write-only `PostRepository`.
- `PostDetail` fields are:
  - `PostId postId`
  - `AuthorId authorId`
  - `PostTitle title`
  - `PostContentType contentType`
  - `PostContent content`
  - `PostSummary summary`
  - `List<TagName> tags`
  - `PostStatus status`

Important wiring note:

- `CreatePostService` is still a pure Java class.
- `PostDetailQueryService` is still a pure Java class.
- They are not annotated with `@Service`.
- `backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java` exposes them as Spring beans for the API layer.

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
- `JpaPostDetailQueryRepositoryAdapter`

Behavior:

- `JpaPostRepositoryAdapter` implements application port `PostRepository`.
- `JpaPostRepositoryAdapter.save(post)` rejects null post with `Post must not be null.`
- `save(post)` is annotated with `@Transactional`.
- `save(post)` maps domain `Post` to `JpaPostEntity`.
- It persists tags through normalized tag names.
- Existing tags are reused by `SpringDataJpaTagRepository.findByName(name)`.
- New tags are created with `new JpaTagEntity(tagName.value())`.
- It saves the post and returns `new PostId(savedEntity.id())`.
- It persists `PostContentType` as `posts.content_type` with `EnumType.STRING`.
- `JpaPostDetailQueryRepositoryAdapter` implements application port `PostDetailQueryRepository`.
- `JpaPostDetailQueryRepositoryAdapter.findPublishedById(postId)` is annotated with `@Transactional(readOnly = true)`.
- `findPublishedById(postId)` queries by ID and `PostStatus.PUBLISHED`.
- `DRAFT` and `HIDDEN` rows are not returned by the public detail query.
- The read adapter maps `JpaPostEntity` to `PostDetail`.
- Tags in `PostDetail` are returned sorted by tag name ascending.

Table mapping:

- `posts`
  - `id`
  - `author_id`
  - `title`
  - `content_type`
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

- No public post list repository/query method yet.
- No update/delete repository method yet.
- No auditing columns yet.
- No soft delete/view count/published at/cover image persistence yet.
- No Flyway/Liquibase yet.
- No migration exists for `posts.content_type`; tests rely on Hibernate `ddl-auto=create-drop`.
- Repository tests use MySQL Testcontainers instead of H2.

### Blog API Behavior

Package:

```text
backend/src/main/java/com/dddblog/backend/blog/api
```

Implemented:

- `PostController`
- `PostRequest`
- `PostResponse`
- `PostApiService`
- `PostDetailResponse`
- `PostDetailApiService`

Behavior:

- `POST /api/posts` requires JWT Bearer authentication.
- Request body fields are:
  - `title`
  - `contentType`
  - `content`
  - `summary`
  - `tags`
  - `status`
- Request body intentionally has no author/member ID field.
- `PostController` derives the author from `@AuthenticationPrincipal AuthenticatedMember`.
- If the authentication principal is missing or is not `AuthenticatedMember`, the API returns `401 { "message": "Authentication failed." }`.
- `PostApiService` converts the authenticated `MemberId` plus `PostRequest` to `CreatePostCommand`.
- The current API accepts only `contentType = MARKDOWN`.
- `contentType = HTML` or `contentType = null` is rejected with `Post content type must be MARKDOWN.`
- Successful creation returns `201 Created` with `{ "postId": ... }`.
- `IllegalArgumentException` is returned as `400 Bad Request` through `GlobalExceptionHandler`.
- Body-supplied `memberId` is ignored if a client sends one.
- `GET /api/posts/{postId}` is public and does not require JWT Bearer authentication.
- `GET /api/posts/{postId}` returns only `PUBLISHED` posts.
- Missing posts, `DRAFT` posts, and `HIDDEN` posts all return `404 { "message": "Post not found." }`.
- Public detail response fields are:
  - `postId`
  - `authorId`
  - `title`
  - `contentType`
  - `content`
  - `summary`
  - `tags`
  - `status`
- Public detail currently includes only `authorId`; it does not join member nickname/profile data.
- Public detail maps `content_markdown` to response field `content`.
- Public detail returns tags in ascending normalized tag-name order.
- `PostDetailApiService` converts `Long postId` to `PostId`, calls `PostDetailQueryService`, and maps `PostDetail` to `PostDetailResponse`.

Example request:

```json
{
  "title": "DDD 시작하기",
  "contentType": "MARKDOWN",
  "content": "# DDD\n\n본문",
  "summary": "DDD 소개",
  "tags": ["ddd", "tdd"],
  "status": "DRAFT"
}
```

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
- `backend/src/main/java/com/dddblog/backend/blog/config/BlogApplicationConfig.java`
- `backend/src/main/java/com/dddblog/backend/member/config/MemberApplicationConfig.java`

Behavior:

- `GlobalExceptionHandler` maps `IllegalArgumentException` to `400 Bad Request`.
- `GlobalExceptionHandler` maps `AuthenticationFailedException` to `401 Unauthorized`.
- `GlobalExceptionHandler` maps `PostNotFoundException` to `404 Not Found`.
- Error body shape is `{ "message": "..." }`.
- `PasswordConfig` provides a `BCryptPasswordEncoder`.
- `BlogApplicationConfig` wires `CreatePostService` from pure application port `PostRepository`.
- `BlogApplicationConfig` wires `PostDetailQueryService` from pure application port `PostDetailQueryRepository`.
- `MemberApplicationConfig` wires `RegisterMemberService` from pure application ports.
- `SecurityConfig` permits unauthenticated signup, login, and `GET /api/posts/{postId}`.
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

Additional exclusions still true:

- No Flyway/Liquibase migration yet.
- No relation/FK between `posts.author_id` and `members.id` yet.
- No public post list API yet.
- No author-private `DRAFT`/`HIDDEN` detail API yet.
- No post update/delete API yet.
- No HTML body acceptance or sanitize policy yet.
- No cover image/image upload integration yet.

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

Blog API tests:

```text
backend/src/test/java/com/dddblog/backend/blog/api
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
- `FakePostDetailQueryRepository`
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

Authenticated post creation test scenarios include:

- `PostApiService` maps authenticated member ID and request values into `CreatePostCommand`.
- `PostApiService` rejects `HTML` content type with `Post content type must be MARKDOWN.`
- `PostApiService` rejects null content type with `Post content type must be MARKDOWN.`
- `PostController` returns `201 Created` and `postId` for authenticated requests.
- `PostController` passes authenticated member ID as author ID.
- `PostController` ignores body-supplied `memberId`.
- `PostController` returns `401 { "message": "Authentication failed." }` for missing token.
- `PostController` returns `401 { "message": "Authentication failed." }` for non-member authentication principal.
- `PostController` returns `400` for `HTML` content type.
- `PostController` returns `400` for domain input errors such as blank title.
- Vertical integration verifies signup, login, Bearer-token post creation, and persisted post values.

Public post detail read test scenarios include:

- `PostDetailQueryService` returns public post detail.
- `PostDetailQueryService` returns `Post not found.` when no public detail exists.
- `PostDetailQueryService` rejects null post ID.
- `JpaPostDetailQueryRepositoryAdapter` reads `PUBLISHED` posts by ID.
- `JpaPostDetailQueryRepositoryAdapter` does not return `DRAFT` posts.
- `JpaPostDetailQueryRepositoryAdapter` does not return `HIDDEN` posts.
- `JpaPostDetailQueryRepositoryAdapter` returns tags sorted by name ascending.
- `PostDetailApiService` maps `PostDetail` to `PostDetailResponse`.
- `PostDetailApiService` rejects invalid post IDs through `PostId`.
- `PostController` returns `200` for public post detail.
- `PostController` allows public post detail without token.
- `PostController` returns `404 { "message": "Post not found." }` when a post cannot be publicly read.
- `PostController` returns `400` for invalid post ID.
- Vertical integration verifies signup, login, Bearer-token post creation, and tokenless public detail read.

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

Important post creation integration test detail:

- The vertical integration test signs up a member, logs in, then posts to `/api/posts`.
- It asserts `201 Created`.
- It asserts the response contains a numeric `postId`.
- It verifies `posts.author_id` is the signed-up member ID.
- It verifies `posts.content_type` is `MARKDOWN`.
- It verifies `posts.content_markdown` stores the submitted body.
- It verifies `posts.status` stores the submitted status.
- It verifies tags are normalized and connected through `post_tags`.

Important public post detail integration test detail:

- The vertical integration test signs up a member, logs in, then posts a `PUBLISHED` post to `/api/posts`.
- It calls `GET /api/posts/{postId}` without an `Authorization` header.
- It asserts `200 OK`.
- It verifies the response contains `postId`, `authorId`, `title`, `contentType`, `content`, `summary`, sorted tags, and `status`.

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

Most relevant public post detail read API commits now merged into `master` and pushed to `origin/master`:

```text
ab62990 test: localize post detail query fake helper
7079ca3 test: verify public post detail flow
5ad4e01 feat: add public post detail endpoint
edb350c feat: add post detail api service
f4d3569 feat: add post detail query adapter
8fd4753 feat: add post detail query service
4fdc9c5 docs: add post detail read api plan
ee6e2b0 docs: add post detail read api design
```

Most relevant authenticated post creation API commits remain relevant background:

```text
e5c02a4 test: verify authenticated post creation flow
118e40e test: harden post controller auth behavior
44ec85e feat: add post creation controller
e880a49 feat: add post api service
9f984d6 feat: configure blog application service
60866fe feat: persist post content type
98b5810 feat: add post content type
4678bf4 docs: add authenticated post creation api plan
d4d1d9e docs: add authenticated post creation api design
```

Earlier login/JWT implementation commits remain relevant background:

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

Recommended next milestone: public post list API.

```text
GET /api/posts
```

Why this is next:

- The project requirements sequence includes public post list/detail read.
- `POST /api/posts` can now create persisted posts with author ID, content type, body, status, and tags.
- `GET /api/posts/{postId}` can now read public `PUBLISHED` post detail without authentication.
- A list slice will make public content discoverable before update/delete work.

Suggested scope:

- Brainstorm and write a spec for public `GET /api/posts`.
- Prefer a narrow first cut: return `PUBLISHED` posts only.
- Decide pagination defaults, maximum page size, and sort order.
- Decide the first response fields carefully; current detail response includes `authorId` only and there is still no FK or cross-context author display read model.
- Reuse the read-query style introduced by `PostDetailQueryRepository` rather than stretching the write-only `PostRepository`.
- Keep update/delete APIs, publish workflows, comments, media, view counts, and frontend work out of this slice unless explicitly approved.

Do not start without a new task:

- Refresh Token, token reissue, server-side logout/blacklist, member update/delete APIs, Flyway/Liquibase, post/member FK, post update/delete APIs, HTML sanitize/acceptance, media upload, or frontend work.

## Notes For Next Agent

- Start by reading this handoff.
- Then read `docs/requirements.md`, `docs/superpowers/specs/2026-06-27-post-detail-read-api-design.md`, and `docs/superpowers/plans/2026-06-27-post-detail-read-api.md`.
- Start a new feature slice from `master`.
- Use `superpowers:brainstorming` before designing the next read/update/delete feature slice.
- Public post detail read API is merged into `master` and pushed to `origin/master` as of commit `ab62990`.
- Use `superpowers:finishing-a-development-branch` only when explicitly asked to finish the branch.
- The user prefers Korean documentation and Korean test method names.
- Preserve pure `blog.domain`, `blog.application`, `member.domain`, and `member.application` style.
- Do not add unrelated refactors or cleanup.
- Current `master` includes the signup, login/JWT, current-member, authenticated post creation, and public post detail read implementation commits.
- Be careful with helper method names under `backend/src/test/java`: the naming-rule command flags ASCII `void` methods in all test source files, including fakes.
- `FakePostDetailQueryRepository` intentionally uses Korean helper method `저장한다(...)` to avoid the broad test-name regex.
- Be careful not to reintroduce DB-generated member IDs. `members.id` is assigned from `Member.id()`.
- Be careful not to use Spring Data `save(entity)` for create-only member persistence with assigned IDs; `EntityManager.persist(entity)` is intentional.
- Be careful that `PostContentType.HTML` exists in the domain but is rejected at the current API boundary. HTML acceptance needs a separate sanitize/security design.
- Be careful that `posts.content_type` has no Flyway/Liquibase migration yet because the project still relies on Hibernate schema generation in tests.
- `GET /api/posts/{postId}` is public only for `PUBLISHED` posts. Missing, `DRAFT`, and `HIDDEN` all map to `Post not found.`.
- Public detail includes only `authorId`, not author nickname/profile.
- Known non-blocking issue: full test runs may print Hibernate schema-drop noise from shared MySQL Testcontainers + `ddl-auto=create-drop`, but the Gradle test result should still be successful.
