# 인증된 글 작성 API 설계

## 1. 배경

DDD Blog는 회원가입, 로그인, JWT 인증, 내 정보 조회까지 구현되어 있다. Blog Context에는 이미 `Post` 도메인, `CreatePostService`, `PostRepository` 포트, JPA 저장 adapter가 존재한다.

다음 목표는 인증된 회원이 HTTP API로 글을 작성하는 세로 슬라이스를 연결하는 것이다.

이번 설계는 본문 형식 확장을 고려한다. 초기 구현은 Markdown 본문만 허용하지만, 추후 HTML 본문을 추가할 수 있도록 API와 도메인에는 본문 형식 개념을 둔다.

## 2. 목표

- 인증된 회원이 `POST /api/posts`로 새 글을 작성할 수 있다.
- 작성자 ID는 요청 본문이 아니라 JWT 인증 주체에서 가져온다.
- 요청은 본문 형식과 본문 값을 분리해서 받는다.
- 이번 API에서는 `MARKDOWN` 본문만 허용한다.
- 생성 성공 시 `201 Created`와 생성된 `postId`를 반환한다.
- 기존 `CreatePostService`와 `JpaPostRepositoryAdapter`를 활용한다.
- Blog 도메인과 애플리케이션 계층은 Spring annotation 없이 순수 Java 스타일을 유지한다.

## 3. 제외 범위

- 글 조회, 수정, 삭제 API
- HTML 본문 허용
- HTML sanitize 정책
- Markdown 렌더링
- 대표 이미지와 이미지 업로드
- `posts.author_id`와 `members.id` FK 추가
- Flyway/Liquibase migration 도입
- 프론트엔드 작업

## 4. API 계약

Endpoint:

```text
POST /api/posts
```

인증:

- JWT Bearer token이 필요하다.
- `AuthenticatedMember.memberId`를 작성자 ID로 사용한다.
- 요청 본문에 작성자 ID를 받지 않는다.

요청:

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

응답:

```json
{
  "postId": 1
}
```

성공 상태:

- `201 Created`

## 5. API 모델

`PostRequest`는 글 작성 요청을 표현한다.

필드:

- `String title`
- `PostContentType contentType`
- `String content`
- `String summary`
- `List<String> tags`
- `PostStatus status`

`PostResponse`는 생성 결과를 표현한다.

필드:

- `Long postId`

요청 필드명은 `contentMarkdown`이 아니라 `content`를 사용한다. 본문 값 자체를 특정 형식에 묶지 않고, 형식은 `contentType`으로 표현한다.

## 6. 도메인 설계

`PostContentType` enum을 추가한다.

초기 값:

- `MARKDOWN`
- `HTML`

`Post` aggregate는 `PostContentType contentType`을 가진다.

규칙:

- `contentType`은 `null`일 수 없다.
- `contentType`이 `null`이면 `Post content type must not be null.` 메시지로 실패한다.
- `PostContent`는 기존처럼 본문 문자열의 null/blank 검증만 담당한다.
- 형식별 sanitize, 렌더링, 허용 태그 정책은 이번 도메인 규칙에 포함하지 않는다.

`PostContentType.HTML`은 도메인 표현으로는 존재하지만, 이번 HTTP API에서는 허용하지 않는다. 이는 추후 HTML 본문 정책을 별도 슬라이스로 설계하기 위함이다.

## 7. 애플리케이션 설계

`CreatePostCommand`에 `PostContentType contentType` 필드를 추가한다.

필드:

- `Long authorId`
- `String title`
- `PostContentType contentType`
- `String content`
- `String summary`
- `List<String> tags`
- `PostStatus status`

`CreatePostService`는 command의 `contentType`을 `Post` 생성자로 전달한다. command null 검증, 도메인 값 객체 변환, repository 저장 흐름은 기존 정책을 유지한다.

`CreatePostService`는 Spring annotation을 붙이지 않는다. Spring Bean 노출은 별도 configuration에서 담당한다.

## 8. API 계층 설계

패키지:

```text
com.dddblog.backend.blog.api
```

추가 클래스:

- `PostController`
- `PostRequest`
- `PostResponse`
- `PostApiService`

`PostController` 책임:

- `POST /api/posts` 요청을 받는다.
- `@AuthenticationPrincipal AuthenticatedMember`를 받는다.
- 인증 주체가 없으면 `AuthenticationFailedException`을 던진다.
- 요청 처리는 `PostApiService`에 위임한다.
- 성공 시 `201 Created`를 반환한다.

`PostApiService` 책임:

- 현재 API에서 `contentType`이 `MARKDOWN`인지 검증한다.
- 인증 member ID와 request를 `CreatePostCommand`로 변환한다.
- `CreatePostService.create(command)`를 호출한다.
- `PostResponse`를 반환한다.

HTML 요청 거부 메시지:

```text
Post content type must be MARKDOWN.
```

## 9. Spring 설정

`BlogApplicationConfig`를 추가해 순수 애플리케이션 서비스를 Bean으로 노출한다.

패키지:

```text
com.dddblog.backend.blog.config
```

Bean:

```java
CreatePostService createPostService(PostRepository postRepository)
```

`JpaPostRepositoryAdapter`는 이미 `PostRepository`를 구현하고 `@Repository`로 등록되어 있으므로 이 Bean에 주입될 수 있다.

## 10. 영속성 설계

`JpaPostEntity`에 `contentType` 필드를 추가한다.

컬럼:

```text
posts.content_type
```

매핑:

- `@Enumerated(EnumType.STRING)`
- `@Column(name = "content_type", nullable = false)`

기존 `content_markdown` 컬럼은 유지한다. 이번 슬라이스에서는 본문 저장 컬럼명을 일반화하지 않는다. 컬럼명 정리는 migration 정책과 함께 별도로 다룬다.

`JpaPostRepositoryAdapter.save(post)`는 `post.contentType()`을 entity에 전달한다.

## 11. 오류 처리

기존 공통 오류 정책을 따른다.

- 인증 실패: `AuthenticationFailedException` -> `401 { "message": "Authentication failed." }`
- 입력 오류: `IllegalArgumentException` -> `400 { "message": "..." }`

대표 실패:

- 토큰 없음: `401 Authentication failed.`
- 인증 주체 없음: `401 Authentication failed.`
- `contentType`이 `HTML`: `400 Post content type must be MARKDOWN.`
- 제목 blank: `400 Post title must not be blank.`
- 본문 blank: `400 Post content must not be blank.`
- 태그 10개 초과: `400 Post tags must be 10 or less.`
- 중복 태그: `400 Post tags must not be duplicated.`

Jackson enum 파싱 실패 같은 JSON 형식 오류는 이번 슬라이스의 핵심 검증 대상에서 제외한다.

## 12. 테스트 계획

모든 백엔드 테스트 메서드명은 한글 시나리오형으로 작성하고 단어 사이를 `_`로 연결한다.

### 12.1 도메인 테스트

- `PostContentType` 값이 존재한다.
- 글을_생성하면_본문_형식을_가진다
- 본문_형식이_null이면_생성할_수_없다

순수 도메인 테스트는 Spring Context를 띄우지 않는다.

### 12.2 애플리케이션 테스트

- 글을_생성하면_본문_형식을_저장소에_전달한다
- 기존 `CreatePostServiceTest`의 command 생성부에 `contentType`을 추가한다.

순수 애플리케이션 테스트는 Spring Context를 띄우지 않는다.

### 12.3 영속성 테스트

- 글을_저장하면_본문_형식이_posts에_저장된다
- 기존 `JpaPostRepositoryAdapterTest`의 저장값 검증에 `contentType`을 포함한다.

JPA 테스트는 기존처럼 MySQL Testcontainers를 사용한다.

### 12.4 API 테스트

`PostControllerTest`를 추가한다.

필수 시나리오:

- 인증된_요청이면_글을_생성하고_201과_글_ID를_반환한다
- 인증된_회원_ID를_작성자_ID로_사용한다
- 토큰이_없으면_401을_반환한다
- HTML_본문_형식이면_400을_반환한다
- 제목이_blank이면_400을_반환한다

### 12.5 통합 테스트

`PostApiIntegrationTest`를 추가한다.

필수 시나리오:

- 회원가입_후_로그인하면_발급된_토큰으로_글을_작성할_수_있다

검증:

- 응답이 `201 Created`다.
- 응답에 `postId`가 있다.
- `posts.author_id`가 로그인한 회원 ID다.
- `posts.content_type`이 `MARKDOWN`이다.
- `posts.content_markdown`에 요청 본문이 저장된다.
- `posts.status`가 요청 상태로 저장된다.
- 태그가 정규화되어 저장된다.

## 13. 검증 명령

백엔드 테스트:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --rerun-tasks
```

테스트명 규칙:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

예상 결과: 출력 없음.

순수 계층 annotation 침투 확인:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional|@Configuration|@Bean'
```

예상 결과: 출력 없음.

H2 재도입 확인:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api\backend
rg -n "h2database|jdbc:h2|H2Dialect|com\.h2database" .
```

예상 결과: 매치 없음. `rg`는 매치가 없으면 exit code `1`을 반환한다.

Whitespace 확인:

```powershell
cd C:\dev\study\ddd-blog\.worktrees\authenticated-post-create-api
git diff --check
```

예상 결과: 출력 없음.

## 14. 성공 기준

- `POST /api/posts`가 인증된 요청에서 글을 생성한다.
- 작성자 ID는 요청 본문이 아니라 인증 member ID로 저장된다.
- `contentType`과 `content`로 본문 형식을 추상화한다.
- 이번 API는 `MARKDOWN`만 허용한다.
- 도메인과 애플리케이션 계층은 Spring/JPA annotation 없이 유지된다.
- API, 애플리케이션, 도메인, 영속성, 통합 테스트가 작성된다.
- 전체 백엔드 테스트가 통과한다.
