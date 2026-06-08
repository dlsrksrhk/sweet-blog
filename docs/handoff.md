# DDD Blog Handoff

## Current State

- Workspace: `C:\dev\study\ddd-blog`
- Backend: `C:\dev\study\ddd-blog\backend`
- Java for this project: `C:\java\jdk-21`
- Spring Boot: `3.5.0`
- Current branch: `master`
- Working tree before handoff update: clean
- Latest implementation commit: `b39dfee fix: avoid ascii void helper names`
- Latest docs commits before this handoff update:
  - `77a0e56 docs: add register member application plan`
  - `3ed9631 docs: add register member application design`

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
- `docs/superpowers/specs/2026-06-08-register-member-application-design.md`
- `docs/superpowers/plans/2026-06-08-register-member-application.md`

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

- No member JPA entity/repository yet.
- No signup API yet.
- No BCrypt/password encoder integration yet.
- No login/JWT integration yet.
- No persistence-backed login ID or nickname duplicate check yet.
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
- `MemberRepository.nextId()` is called only after command validation and duplicate checks pass.
- `Member.register(memberId, name, nickname, loginId, passwordHash)` creates the new member.
- `MemberRepository.save(member)` is called and its returned `MemberId` is returned from the service.

`MemberRepository` methods:

```java
boolean existsByLoginId(LoginId loginId);
boolean existsByNickname(Nickname nickname);
MemberId nextId();
MemberId save(Member member);
```

Important wiring note:

- `RegisterMemberService` is still a pure Java class.
- It is not annotated with `@Service`.
- There is not yet a Spring configuration that exposes `RegisterMemberService` as a bean.
- A future API/config slice must decide how to wire it.

Important design note:

- Current member registration requires `MemberId` before calling `Member.register(...)`.
- For the application slice, ID generation was placed behind `MemberRepository.nextId()`.
- This is intentionally conservative for the pure Java service, but it creates a design decision for the next persistence slice because MySQL/JPA auto-increment normally generates IDs during `save`, not before `save`.
- The next design session should explicitly decide whether to keep preallocated IDs, introduce a separate ID generator, or refactor member registration to allow persistence-generated IDs.

Member application package is pure Java:

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

Member application tests:

```text
backend/src/test/java/com/dddblog/backend/member/application
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
- `FakeMemberRepository`

Member application test:

- `RegisterMemberServiceTest`

Member application test scenarios:

- `유효한_요청이면_회원을_저장하고_ID를_반환한다`
- `저장된_회원은_요청_값을_도메인_값으로_가진다`
- `신규_회원은_MEMBER_권한과_ACTIVE_상태를_가진다`
- `command가_null이면_저장하지_않는다`
- `로그인_ID가_이미_존재하면_저장하지_않는다`
- `닉네임이_이미_존재하면_저장하지_않는다`
- `잘못된_로그인_ID이면_저장하지_않는다`
- `잘못된_닉네임이면_저장하지_않는다`
- `잘못된_비밀번호_해시이면_저장하지_않는다`

Important member application test detail:

- `FakeMemberRepository` helper methods intentionally return `FakeMemberRepository` instead of `void`.
- This avoids matching the project-wide ASCII `void ...` test naming scan, which scans all backend test Java files, not only `*Test.java`.

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
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional'
```

Expected: no output.

## Recent Commits

Most relevant recent commits:

```text
b39dfee fix: avoid ascii void helper names
dcb9d5c feat: reject duplicate member registration values
30fc52a test: reject invalid member registration command
1bfccb4 test: verify registered member values
c1185ba feat: add member registration service
77a0e56 docs: add register member application plan
3ed9631 docs: add register member application design
234fb74 fix: redact password hash string representation
0339e15 feat: add member aggregate
dd6d178 feat: add member password hash and enums
d5cd5bc feat: add member profile value objects
480e7aa feat: add member id value object
```

## Recommended Next Step

Recommended next brainstorming topic:

```text
회원 저장소 영속성 1차와 회원 ID 생성 방식
```

Why this is recommended:

- Pure `Member` domain is implemented.
- Pure `RegisterMemberService` application use case is implemented.
- The next natural DDD/TDD slice is to connect `MemberRepository` to persistence before adding signup API/auth.
- The current `MemberRepository.nextId()` choice must be reconciled with JPA/MySQL ID generation before implementing a real adapter.

Recommended scope:

- Decide the ID generation approach first.
- If keeping current `MemberRepository.nextId()`:
  - Design how a JPA adapter can allocate a valid positive `MemberId` before saving.
  - Consider whether a custom ID table/sequence-like strategy is acceptable for this learning project.
- If preferring JPA/MySQL generated IDs:
  - Consider refactoring `Member` or the registration flow so a new member can be persisted before receiving a final ID.
  - This may require changing `MemberRepository` and `RegisterMemberService`.
- After the ID approach is approved, design the member persistence adapter:
  - `member.persistence` package
  - JPA entity for `members`
  - Spring Data repository
  - Adapter implementing `MemberRepository`
  - Duplicate lookup by login ID and nickname
  - Save behavior returning `MemberId`
- Keep signup API, BCrypt, login, JWT, and Post FK out of this slice unless explicitly approved.
- Use H2-backed `@DataJpaTest` style similar to `JpaPostRepositoryAdapterTest` if persistence is implemented.

Likely questions to ask during brainstorming:

1. Should member IDs continue to be allocated before `Member.register(...)`, or should the member model/application flow be adjusted for persistence-generated IDs?
2. If IDs stay preallocated, should `nextId()` remain on `MemberRepository`, or move to a separate `MemberIdGenerator` port?
3. Should the first member persistence slice use H2 only, or introduce Testcontainers MySQL now?
4. Should `members.login_id` and `members.nickname` have unique constraints in the JPA mapping immediately?
5. Should `PasswordHash` be stored as-is in `members.password_hash`, with no BCrypt integration yet?
6. Should `RegisterMemberService` continue returning `memberRepository.save(member)`, or return the preallocated `memberId` after a void save?

Alternative next step:

```text
회원가입 API 1차
```

This is possible, but persistence and password hashing/wiring decisions would need to be addressed. A member persistence slice first is the cleaner next step.

## Notes For Next Agent

- The user wants to continue in a new chat.
- Start by reading this handoff.
- Then run `superpowers:brainstorming` for `회원 저장소 영속성 1차와 회원 ID 생성 방식`.
- Do not implement immediately.
- Ask one clarifying question at a time.
- The user prefers Korean documentation and Korean test method names.
- Preserve pure domain/application style unless a new approved spec changes it.
- Do not add unrelated refactors or cleanup.
- Use an isolated worktree before implementation.
- Current `master` includes the completed member domain slice and the completed member registration application slice.
- Be careful with helper method names under `backend/src/test/java`: the naming-rule command flags ASCII `void` methods in all test source files, including fakes.
