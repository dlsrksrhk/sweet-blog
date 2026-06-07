# DDD Blog Handoff

## Current State

- Workspace: `C:\dev\study\ddd-blog`
- Backend: `C:\dev\study\ddd-blog\backend`
- Java for this project: `C:\java\jdk-21`
- Spring Boot: `3.5.0`
- Current branch: `master`
- Working tree before handoff update: clean
- Latest implementation commit: `234fb74 fix: redact password hash string representation`
- Latest docs commits:
  - `04cf81a docs: add member domain plan`
  - `49f2710 docs: add member domain design`

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
- For implementation plans, use an isolated worktree before coding.

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
- `MemberRole` values are `MEMBER`, `ADMIN`.
- `MemberStatus` values are `ACTIVE`, `INACTIVE`.

Important exclusions still true:

- No member application service yet.
- No `MemberRepository` port yet.
- No member JPA entity/repository yet.
- No signup API yet.
- No BCrypt/password encoder integration yet.
- No login/JWT integration yet.
- No login ID or nickname duplicate check yet.
- No relation/FK between `posts.author_id` and a member table yet.

Member domain package is pure Java:

- No Spring annotations.
- No JPA annotations.

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

Blog domain tests:

```text
backend/src/test/java/com/dddblog/backend/blog/domain
```

Member domain tests:

```text
backend/src/test/java/com/dddblog/backend/member/domain
```

Blog application tests:

```text
backend/src/test/java/com/dddblog/backend/blog/application
```

Blog persistence tests:

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
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional'
```

Expected: no output.

## Recent Commits

Most relevant recent commits:

```text
234fb74 fix: redact password hash string representation
0339e15 feat: add member aggregate
dd6d178 feat: add member password hash and enums
d5cd5bc feat: add member profile value objects
480e7aa feat: add member id value object
04cf81a docs: add member domain plan
49f2710 docs: add member domain design
9e31656 docs: update handoff for create post api
ad3f45c fix: make post persistence transactional
1f5973a test: reject null post persistence
898c45a test: verify post tag reuse
d9ebf2c test: reload persisted post values
```

## Recommended Next Step

Recommended next brainstorming topic:

```text
회원가입 애플리케이션 서비스 1차
```

Why this is recommended:

- The user explicitly chose to develop `Member` before the first post creation API.
- Pure `Member` domain is now implemented.
- The next natural DDD/TDD slice is to wrap member creation in an application use case before adding JPA/API/auth.

Recommended scope:

- Add a `member.application` package.
- Add a `RegisterMemberCommand`.
- Add a `RegisterMemberService`.
- Add a `MemberRepository` application port.
- Add a test fake repository under `src/test`.
- Keep `RegisterMemberService` pure Java, with no Spring annotation.
- Keep BCrypt/password hashing out of this slice unless explicitly approved.
- For this slice, accept an already-hashed password through the command as `passwordHash`.
- Check duplicate login ID and duplicate nickname through repository-port methods.
- Return `MemberId` after successful registration.
- Avoid JPA persistence, API, login, JWT, password encoder, and security configuration in this slice.
- Keep tests Korean scenario-style and Spring Context-free.

Suggested conservative path:

- `RegisterMemberCommand` fields:
  - `Long memberId`
  - `String name`
  - `String nickname`
  - `String loginId`
  - `String passwordHash`
- `MemberRepository` methods:
  - `boolean existsByLoginId(LoginId loginId)`
  - `boolean existsByNickname(Nickname nickname)`
  - `MemberId save(Member member)`
- `RegisterMemberService.register(command)` flow:
  1. Reject null command.
  2. Convert primitives to value objects.
  3. Check duplicate login ID.
  4. Check duplicate nickname.
  5. Create `Member.register(...)`.
  6. Save through repository.
  7. Return saved `MemberId`.
- Test with `FakeMemberRepository`.

Likely questions to ask during brainstorming:

1. Should `RegisterMemberCommand` receive `memberId` for now, or should repository/fake generate it?
2. Should this slice accept `passwordHash` directly, or introduce a password-hashing port now?
3. Should duplicate login ID/nickname exceptions use plain `IllegalArgumentException` first, or introduce named domain/application exceptions?
4. Should `MemberRepository.save(Member)` return `MemberId`, matching current `PostRepository.save(Post)` style?
5. Should exact boundary-allowed tests be added for `MemberName`, `Nickname`, and `LoginId` now, or leave them as a later test-hardening task?

Recommended answer defaults if the user wants the conservative path:

- Let repository/fake generate `MemberId` if registration should resemble persistence-generated IDs.
- If minimizing changes, include `memberId` in command for now; but this is less realistic.
- Keep password hashing out for one more slice and accept `passwordHash`.
- Use `IllegalArgumentException` for duplicate checks initially.
- Use `MemberRepository.save(Member)` returning `MemberId`, consistent with `PostRepository.save(Post)`.
- Add boundary-allowed tests only if the user wants to harden existing value-object coverage before moving into application service.

Alternative next step:

```text
글 작성 API 1차
```

This was the previous recommendation, but after choosing and completing `Member` domain first, member registration application service is now the cleaner next slice.

## Notes For Next Agent

- The user wants to continue in a new chat.
- Start by reading this handoff.
- Then run `superpowers:brainstorming` for `회원가입 애플리케이션 서비스 1차`.
- Do not implement immediately.
- Ask one clarifying question at a time.
- The user prefers Korean documentation and Korean test method names.
- Preserve pure domain/application style unless a new approved spec changes it.
- Do not add unrelated refactors or cleanup.
- Use an isolated worktree before implementation.
- Current `master` includes the completed member domain slice.
