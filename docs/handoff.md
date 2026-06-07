# DDD Blog Handoff

## Current State

- Workspace: `C:\dev\study\ddd-blog`
- Backend: `C:\dev\study\ddd-blog\backend`
- Java for this project: `C:\java\jdk-21`
- Spring Boot: `3.5.0`
- Current branch: `master`
- Working tree at handoff time: clean
- Latest feature commit: `bc3002c feat: add create post service`

## Project Rules

See `AGENTS.md`.

Important local rules:

- Backend test method names must be Korean scenario-style names.
- Korean test method words are joined with `_`.
- Pure domain/application tests must not start Spring Context.
- Domain/application classes in current learning slices should not use Spring or JPA annotations.
- Prefer TDD: write a small failing test first, then implement the minimum code to pass.

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
- `PostId` is only used as the repository save result for now.
- `Post` validates author/title/content/status.
- `Post` normalizes null summary through `PostSummary`.
- `Post` treats null tag list as empty.
- `Post` rejects null tag elements.
- `Post` rejects more than 10 tags.
- `Post` rejects duplicate normalized tags.
- `TagName` trims and lowercases with `Locale.ROOT`.

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

### Tests

Domain tests:

```text
backend/src/test/java/com/dddblog/backend/blog/domain
```

Application tests:

```text
backend/src/test/java/com/dddblog/backend/blog/application
```

Implemented application test fake:

- `FakePostRepository`

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

Check no Spring/JPA annotations in blog package:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table'
```

Expected: no output.

## Recent Commits

Most relevant recent commits:

```text
bc3002c feat: add create post service
0c0fec5 docs: clarify post id validation messages
b83ed95 test: clarify null post id validation
1d557c0 feat: add post id value object
5d78dd8 docs: add create post application plan
3895de4 docs: add create post application design
15c6ea4 test: reject duplicate post tags
133e690 test: reject null post tags
08b7cbb feat: add post aggregate
```

## Recommended Next Step

Recommended next brainstorming topic:

```text
JPA 매핑 + Repository 구현 1차
```

Suggested scope:

- Keep current `PostRepository` application port.
- Add persistence adapter in a separate package.
- Add JPA entity or entities for post persistence.
- Add mapper between domain `Post` and JPA entity.
- Use H2 or Testcontainers decision during brainstorming.
- Avoid Controller/API/JWT in this slice.
- Keep tests Korean scenario-style.

Likely questions to ask during the next brainstorming:

1. Should the first persistence test use H2 for speed or Testcontainers MySQL for realism?
2. Should `Post` remain ID-less in domain for now, or should saved/read model introduce a separate representation?
3. Should tags be embedded as simple element collection first, or modeled as separate tag table immediately?

Recommended conservative path:

- Start with a persistence adapter and repository integration test.
- Use H2 only if the user wants faster learning feedback.
- Use Testcontainers MySQL if the user wants stronger JPA/MySQL realism.

## Notes For Next Agent

- Do not rewrite existing domain/application style.
- Do not add Spring annotations to the current domain/application service classes unless a new spec explicitly changes that rule.
- Before implementing the next slice, run `superpowers:brainstorming`.
- After approved design, write a Korean spec under `docs/superpowers/specs`.
- Then use `superpowers:writing-plans`.
- The user prefers Korean documentation and Korean test method names.
