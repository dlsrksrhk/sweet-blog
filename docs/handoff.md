# DDD Blog Handoff

## Current State

- Main workspace: `C:\dev\study\ddd-blog`
- Active implementation worktree: `C:\dev\study\ddd-blog\.worktrees\member-persistence-id-generation`
- Backend: `C:\dev\study\ddd-blog\.worktrees\member-persistence-id-generation\backend`
- Java for this project: `C:\java\jdk-21`
- Spring Boot: `3.5.0`
- Current implementation branch: `codex/member-persistence-id-generation`
- Implementation plan Tasks 1-5 are complete.
- Working tree after final handoff update: clean
- Latest code commit: `cd7dd9d fix: initialize member id sequence before use`
- Latest plan/design commits:
  - `c4508df docs: add member persistence id generation plan`
  - `32eda6f docs: add member persistence id generation design`

## Project Rules

See `AGENTS.md`.

Important local rules:

- Backend test method names must be Korean scenario-style names.
- Korean test method words are joined with `_`.
- Pure domain/application tests must not start Spring Context.
- Domain/application classes in current learning slices should not use Spring or JPA annotations.
- Spring/JPA annotations are allowed in adapter packages such as `blog.persistence` and will likely be allowed in API/config packages after explicit design approval.
- Prefer TDD: write a small failing test first, then implement the minimum code to pass.
- For new feature slices, use `superpowers:brainstorming`, write a Korean spec, then use `superpowers:writing-plans`.
- Current slice already has approved spec and plan. Continue with the existing implementation plan; do not restart brainstorming.
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

- `RegisterMemberService` is still a pure Java class.
- It is not annotated with `@Service`.
- There is not yet a Spring configuration that exposes `RegisterMemberService` as a bean.
- A future API/config slice must decide how to wire it.

Important design note:

- Current member registration requires `MemberId` before calling `Member.register(...)`.
- ID generation is now behind the separate `MemberIdGenerator` port.
- The design decision is complete: member IDs stay preallocated before `Member.register(...)`.
- The real DB-backed `MemberIdGenerator` is implemented in the persistence package.

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
- Repository tests now use MySQL Testcontainers instead of H2.

### Member Persistence Layer

Package:

```text
backend/src/main/java/com/dddblog/backend/member/persistence
```

Implemented:

- `JpaMemberEntity`
- `SpringDataJpaMemberRepository`
- `JpaMemberRepositoryAdapter`

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

Important member persistence details:

- `members.id` is intentionally assigned, not auto-increment.
- DB-backed ID generation is implemented with a `member_id_sequences` table.
- `JpaMemberIdSequenceEntity`, `SpringDataJpaMemberIdSequenceRepository`, and `JpaMemberIdGenerator` are complete for Task 4.
- `JpaMemberIdGenerator` initializes the `member` sequence row before first use so pessimistic locking protects ID increments.
- No member read/update/delete repository methods yet.
- No Flyway/Liquibase migration yet.
- No relation/FK between `posts.author_id` and `members.id` yet.

### Dependencies

Backend Gradle dependencies include:

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `spring-boot-starter-web`
- `mysql-connector-j`
- `org.testcontainers:junit-jupiter` as `testImplementation`
- `org.testcontainers:mysql` as `testImplementation`

H2 was removed from test runtime dependencies during the Testcontainers migration.

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

Member persistence tests:

```text
backend/src/test/java/com/dddblog/backend/member/persistence
```

JPA test support:

```text
backend/src/test/java/com/dddblog/backend/support/MysqlDataJpaTestSupport.java
```

Implemented application test fake:

- `FakePostRepository`
- `FakeMemberRepository`
- `FakeMemberIdGenerator`

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

Implemented persistence tests:

- `JpaPostRepositoryAdapterTest`
- `JpaMemberRepositoryAdapterTest`

Blog persistence test scenarios:

- `글을_저장하면_ID를_반환한다`
- `글을_저장하면_본문_값이_posts에_저장된다`
- `이미_존재하는_태그는_새로_만들지_않고_재사용한다`
- `글이_null이면_저장할_수_없다`

Member persistence test scenarios:

- `회원을_저장하면_ID를_반환한다`
- `회원을_저장하면_members에_도메인_값이_저장된다`
- `저장된_로그인_ID가_있으면_존재한다고_판단한다`
- `저장된_닉네임이_있으면_존재한다고_판단한다`
- `로그인_ID는_중복_저장할_수_없다`
- `닉네임은_중복_저장할_수_없다`
- `같은_ID의_회원은_중복_저장할_수_없다`
- `회원이_null이면_저장할_수_없다`

Important persistence test detail:

- JPA tests use MySQL Testcontainers through `MysqlDataJpaTestSupport`.
- Docker must be running for persistence tests.
- Persisted value tests use `TestEntityManager.flush()` and `clear()` before reloading saved rows.
- This avoids only verifying the first-level persistence context.
- Full test runs currently pass, but Hibernate may log non-blocking schema-drop noise during shutdown because multiple JPA slice contexts share the static MySQL test container with `ddl-auto=create-drop`.

All backend test method names are Korean scenario-style.

## Verification Commands

Run from backend:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\member-persistence-id-generation\backend
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
cd C:\dev\study\ddd-blog\.worktrees\member-persistence-id-generation\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected: no output.

Check no Spring/JPA annotations in pure domain/application packages:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\member-persistence-id-generation\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional'
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional'
```

Expected: no output.

## Recent Commits

Most relevant recent commits on `codex/member-persistence-id-generation`:

```text
cd7dd9d fix: initialize member id sequence before use
2d92cc2 fix: align mysql test support helper naming
da643de test: verify member id generator sequence values
0b37e64 feat: add jpa member id generator
1c8b383 docs: update handoff after member persistence task 3
4680e16 fix: use insert semantics for member save
81c6460 fix: persist generated member id
5c1b360 feat: add jpa member repository adapter
e384b83 test: run jpa tests with mysql testcontainers
dc52a10 refactor: split member id generator port
c4508df docs: add member persistence id generation plan
32eda6f docs: add member persistence id generation design
743338b docs: update handoff for member registration
```

## Recommended Next Step

Finish the development branch after final verification:

```text
docs/superpowers/plans/2026-06-09-member-persistence-id-generation.md
```

Completed Task 4 files:

- `backend/src/test/java/com/dddblog/backend/member/persistence/JpaMemberIdGeneratorTest.java`
- `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberIdSequenceEntity.java`
- `backend/src/main/java/com/dddblog/backend/member/persistence/SpringDataJpaMemberIdSequenceRepository.java`
- `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberIdGenerator.java`

Implemented Task 4 behavior:

- Implement `JpaMemberIdGenerator` as `MemberIdGenerator`.
- Use table `member_id_sequences`.
- Use row `name = 'member'`.
- The `member` sequence row is initialized before first use.
- First `nextId()` call returns `new MemberId(1L)`.
- Consecutive calls return increasing IDs.
- Repository lookup should use pessimistic write lock with explicit `@Query`.

Do not start:

- Signup API, BCrypt, login/JWT, Flyway/Liquibase, member read/update/delete, or Post/member FK.

## Notes For Next Agent

- The user wants to continue in a new chat.
- Start by reading this handoff.
- Then read `docs/superpowers/plans/2026-06-09-member-persistence-id-generation.md`.
- Do not restart brainstorming or write a new plan.
- Continue in worktree `C:\dev\study\ddd-blog\.worktrees\member-persistence-id-generation` on branch `codex/member-persistence-id-generation`.
- Task 1, Task 2, Task 3, and Task 4 are complete and reviewed.
- Task 5 verification has passed locally.
- Use `superpowers:subagent-driven-development` if continuing the same workflow.
- The user prefers Korean documentation and Korean test method names.
- Preserve pure domain/application style.
- Do not add unrelated refactors or cleanup.
- Current `master` does not include this branch's implementation commits yet.
- Be careful with helper method names under `backend/src/test/java`: the naming-rule command flags ASCII `void` methods in all test source files, including fakes.
- Be careful not to reintroduce DB-generated member IDs. `members.id` is assigned from `Member.id()`.
- Be careful not to use Spring Data `save(entity)` for create-only member persistence with assigned IDs; `EntityManager.persist(entity)` is intentional.
- Known non-blocking issue: full test runs may print Hibernate schema-drop noise from shared MySQL Testcontainers + `ddl-auto=create-drop`, but current targeted and full tests passed during review.
