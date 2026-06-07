# DDD Blog Handoff

## Current State

- Workspace: `C:\dev\study\ddd-blog`
- Backend: `C:\dev\study\ddd-blog\backend`
- Java for this project: `C:\java\jdk-21`
- Spring Boot: `3.5.0`
- Current branch: `master`
- Working tree at handoff time: clean
- Latest feature commit: `ad3f45c fix: make post persistence transactional`
- Latest docs commits:
  - `8e18850 docs: add jpa post repository plan`
  - `4c6ed1d docs: add jpa post repository design`

## Project Rules

See `AGENTS.md`.

Important local rules:

- Backend test method names must be Korean scenario-style names.
- Korean test method words are joined with `_`.
- Pure domain/application tests must not start Spring Context.
- Domain/application classes in current learning slices should not use Spring or JPA annotations.
- Spring/JPA annotations are allowed in adapter packages such as `blog.persistence` and will likely be allowed in API/config packages after explicit design approval.
- Prefer TDD: write a small failing test first, then implement the minimum code to pass.
- Before implementing the next slice, run `superpowers:brainstorming`.
- After approved design, write a Korean spec under `docs/superpowers/specs`.
- Then use `superpowers:writing-plans`.

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

### Domain Layer

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

Important domain behavior:

- `Post` has no persistence ID field yet.
- `PostId` is used as repository save result for now.
- `Post` validates author/title/content/status.
- `Post` normalizes null summary through `PostSummary`.
- `Post` treats null tag list as empty.
- `Post` rejects null tag elements.
- `Post` rejects more than 10 tags.
- `Post` rejects duplicate normalized tags.
- `TagName` trims and lowercases with `Locale.ROOT`.

Domain package is still pure Java:

- No Spring annotations.
- No JPA annotations.

### Application Layer

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
- The next API slice must decide how to wire it.

### Persistence Layer

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
- H2 is used for current repository tests; Testcontainers MySQL is not added yet.

### Dependencies

Backend Gradle dependencies include:

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `spring-boot-starter-web`
- `mysql-connector-j`
- `com.h2database:h2` as `testRuntimeOnly`

### Tests

Domain tests:

```text
backend/src/test/java/com/dddblog/backend/blog/domain
```

Application tests:

```text
backend/src/test/java/com/dddblog/backend/blog/application
```

Persistence tests:

```text
backend/src/test/java/com/dddblog/backend/blog/persistence
```

Implemented application test fake:

- `FakePostRepository`

Implemented persistence test:

- `JpaPostRepositoryAdapterTest`

Persistence test scenarios:

- `글을_저장하면_ID를_반환한다`
- `글을_저장하면_본문_값이_posts에_저장된다`
- `이미_존재하는_태그는_새로_만들지_않고_재사용한다`
- `글이_null이면_저장할_수_없다`

Important persistence test detail:

- The persisted value test uses `TestEntityManager.flush()` and `clear()` before reloading the saved post.
- This avoids only verifying the first-level persistence context.

All backend test method names are Korean scenario-style.

## Verification Commands

Run from backend:

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

Check test naming rule:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected: no output.

Check no Spring/JPA annotations in pure domain/application packages:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional'
```

Expected: no output.

## Recent Commits

Most relevant recent commits:

```text
ad3f45c fix: make post persistence transactional
1f5973a test: reject null post persistence
898c45a test: verify post tag reuse
d9ebf2c test: reload persisted post values
b9ff2a0 test: verify persisted post values
4f195b6 feat: add jpa post repository adapter
8d0b44e fix: add post tag unique constraint
f71583d feat: add jpa post entities
726902c test: add failing jpa post repository test
9a9ea4f build: add h2 test dependency
18d4689 chore: ignore local worktrees
8e18850 docs: add jpa post repository plan
4c6ed1d docs: add jpa post repository design
9b49a7c docs: add project handoff
bc3002c feat: add create post service
```

## Recommended Next Step

Recommended next brainstorming topic:

```text
글 작성 API 1차
```

Recommended scope:

- Add the first HTTP entry point for creating posts.
- Start with `POST /api/posts`.
- Keep current domain/application/persistence style.
- Wire `CreatePostService` into Spring explicitly.
- Use `JpaPostRepositoryAdapter` as the real `PostRepository` implementation.
- Keep JWT/auth out of this slice unless the user explicitly expands scope.
- Decide during brainstorming how to supply `authorId` before JWT exists.
- Avoid post list/detail/read API in this slice.
- Avoid update/delete/status-change API in this slice.
- Keep tests Korean scenario-style.

Suggested conservative path:

- Create an API/controller adapter package separate from domain/application.
- Add request/response DTOs for create post.
- Add a Spring configuration or adapter-specific bean registration for `CreatePostService`.
- For API 1차, include `authorId` in request body as a temporary learning shortcut.
- Return generated `postId` in response.
- Use a focused Spring MVC/API test to verify HTTP request -> command -> service path.
- Add an integration test only if wiring `CreatePostService` + JPA adapter through Spring needs verification.

Likely questions to ask during brainstorming:

1. Before JWT exists, should the first create-post API receive `authorId` in the request body, use a fixed test author ID, or introduce a temporary authentication stub?
2. Should `CreatePostService` become a Spring bean through `@Service`, or should it remain pure and be registered through configuration?
3. Should the first API test use `@WebMvcTest` with a mocked service, or `@SpringBootTest`/full wiring to include the real application service and JPA adapter?
4. What should `POST /api/posts` return first: only `{ "postId": 1 }`, `201 Created` with `Location`, or a fuller response?
5. How should domain `IllegalArgumentException` be translated in the first API slice: simple `400 Bad Request` handler now, or postpone global exception handling?

Recommended answer defaults if the user wants the conservative path:

- Use request body `authorId` temporarily.
- Keep `CreatePostService` pure and register it via configuration.
- Start with `@WebMvcTest` for controller contract plus one Spring wiring/integration test if needed.
- Return `201 Created` and body `{ "postId": <id> }`.
- Add a minimal exception handler for `IllegalArgumentException -> 400 Bad Request` only if the API test needs it.

## Notes For Next Agent

- The user wants to continue in a new chat.
- Start by reading this handoff.
- Then run `superpowers:brainstorming` for `글 작성 API 1차`.
- Do not implement immediately.
- Ask one clarifying question at a time.
- The user prefers Korean documentation and Korean test method names.
- Preserve the pure domain/application style unless a new approved spec changes it.
- Do not add unrelated refactors or cleanup.
- `master` is currently the working branch after the JPA repository slice was merged.
