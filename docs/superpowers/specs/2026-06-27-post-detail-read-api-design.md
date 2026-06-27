# 공개 글 상세 조회 API 설계

## 1. 배경

DDD Blog는 회원가입, 로그인/JWT 인증, 내 정보 조회, 인증된 글 작성 API까지 구현되어 있다. 현재 `POST /api/posts`는 인증된 회원 ID를 작성자로 사용해 글을 저장하지만, 저장된 글을 HTTP로 조회하는 API는 아직 없다.

다음 목표는 공개된 글을 비회원도 상세 조회할 수 있게 하는 첫 번째 read slice다. 이번 slice는 `GET /api/posts/{postId}` 하나로 좁히고, 목록 조회와 작성자 닉네임 조인은 이후 slice로 남긴다.

## 2. 목표

- 비회원이 `GET /api/posts/{postId}`로 공개 글 상세를 조회할 수 있다.
- `PUBLISHED` 상태의 글만 공개 조회에 노출한다.
- 없는 글, `DRAFT` 글, `HIDDEN` 글은 모두 `404 { "message": "Post not found." }`로 응답한다.
- 응답에는 현재 저장되어 있는 글 필드를 중심으로 반환한다.
- 작성자 정보는 이번 slice에서 `authorId`만 포함한다.
- 기존 write port인 `PostRepository`에는 조회 책임을 추가하지 않고, read 전용 query port/service를 둔다.
- `blog.domain`과 `blog.application`은 Spring/JPA annotation 없이 순수 Java 스타일을 유지한다.

## 3. 제외 범위

- `GET /api/posts` 목록 조회
- 작성자 닉네임, 이름, 프로필 정보 조회
- Member Context와 Blog Context 사이 FK 추가
- 공개일, 수정일, 조회수, 대표 이미지 응답
- 상세 조회 시 조회수 증가
- 작성자가 자신의 `DRAFT` 또는 `HIDDEN` 글을 조회하는 인증 API
- 글 수정, 삭제, 공개 상태 변경 API
- Flyway/Liquibase migration 도입
- 프론트엔드 작업

## 4. API 계약

Endpoint:

```text
GET /api/posts/{postId}
```

인증:

- 인증이 필요 없다.
- `SecurityConfig`에서 `GET /api/posts/{postId}`는 `permitAll`로 허용한다.
- 기존 `POST /api/posts`는 계속 인증이 필요하다.

성공 응답:

```json
{
  "postId": 1,
  "authorId": 10,
  "title": "DDD 시작하기",
  "contentType": "MARKDOWN",
  "content": "# DDD\n\n본문",
  "summary": "DDD 소개",
  "tags": ["ddd", "tdd"],
  "status": "PUBLISHED"
}
```

응답 필드:

- `Long postId`
- `Long authorId`
- `String title`
- `PostContentType contentType`
- `String content`
- `String summary`
- `List<String> tags`
- `PostStatus status`

본문 저장 컬럼은 현재 `content_markdown`이지만, API 응답 필드는 작성 API와 일관되게 `content`를 사용한다.

## 5. 조회 규칙

- `postId` path variable은 `PostId` 값 객체로 검증한다.
- `postId`가 `null`이거나 양수가 아니면 기존 `IllegalArgumentException` 정책에 따라 `400 Bad Request`로 응답한다.
- 조회 repository는 `PUBLISHED` 상태만 찾는다.
- `DRAFT`와 `HIDDEN`은 공개 조회에서 존재하지 않는 글처럼 처리한다.
- 조회할 수 없는 글은 `PostNotFoundException`을 던진다.
- `PostNotFoundException`은 `404 Not Found`와 `{ "message": "Post not found." }`로 매핑한다.

## 6. 애플리케이션 설계

패키지:

```text
com.dddblog.backend.blog.application
```

추가 클래스:

- `PostDetail`
- `PostDetailQueryRepository`
- `PostDetailQueryService`
- `PostNotFoundException`

`PostDetail`은 공개 글 상세 조회에 필요한 read model이다.

필드:

- `PostId postId`
- `AuthorId authorId`
- `PostTitle title`
- `PostContentType contentType`
- `PostContent content`
- `PostSummary summary`
- `List<TagName> tags`
- `PostStatus status`

`PostDetailQueryRepository` 메서드:

```java
Optional<PostDetail> findPublishedById(PostId postId);
```

`PostDetailQueryService` 책임:

- null query input을 방어한다.
- `PostDetailQueryRepository.findPublishedById(postId)`를 호출한다.
- 결과가 없으면 `PostNotFoundException`을 던진다.
- 결과가 있으면 `PostDetail`을 반환한다.

순수 애플리케이션 계층 원칙을 유지하기 위해 이 클래스들에는 Spring annotation을 붙이지 않는다. Spring bean 등록은 `BlogApplicationConfig`에서 담당한다.

## 7. 영속성 설계

패키지:

```text
com.dddblog.backend.blog.persistence
```

추가 클래스:

- `JpaPostDetailQueryRepositoryAdapter`

변경 클래스:

- `SpringDataJpaPostRepository`

`JpaPostDetailQueryRepositoryAdapter`는 `PostDetailQueryRepository`를 구현한다. 기존 `JpaPostRepositoryAdapter`는 글 작성 저장 책임을 유지하고, 상세 조회 adapter를 별도로 둔다.

`SpringDataJpaPostRepository`에는 공개 글 상세 조회용 메서드를 추가한다.

예:

```java
Optional<JpaPostEntity> findByIdAndStatus(Long id, PostStatus status);
```

adapter는 `PostStatus.PUBLISHED`로만 조회한다. 조회된 `JpaPostEntity`는 `PostDetail`로 변환한다. tags는 응답 안정성을 위해 이름 기준 오름차순으로 반환한다.

이번 slice에서는 `members`와 join하지 않는다. `authorId`는 `posts.author_id` 값을 그대로 반환한다.

## 8. API 계층 설계

패키지:

```text
com.dddblog.backend.blog.api
```

추가 클래스:

- `PostDetailResponse`
- `PostDetailApiService`

변경 클래스:

- `PostController`

`PostDetailApiService` 책임:

- path variable의 `Long postId`를 `PostId`로 변환한다.
- `PostDetailQueryService`를 호출한다.
- `PostDetail`을 `PostDetailResponse`로 변환한다.

`PostController` 책임:

- `GET /api/posts/{postId}` 요청을 받는다.
- 인증 principal을 요구하지 않는다.
- `PostDetailApiService.getDetail(postId)`에 위임한다.
- 성공 시 `200 OK`와 `PostDetailResponse`를 반환한다.

기존 `PostApiService`는 글 작성 요청 변환 책임을 유지한다. 상세 조회 변환은 `PostDetailApiService`로 분리해 create/read 흐름이 섞이지 않도록 한다.

## 9. 오류 처리

기존 공통 오류 정책을 확장한다.

- `IllegalArgumentException` -> `400 Bad Request`
- `AuthenticationFailedException` -> `401 Unauthorized`
- `PostNotFoundException` -> `404 Not Found`

오류 응답 shape는 기존과 같이 `{ "message": "..." }`를 사용한다.

대표 실패:

- `postId <= 0`: `400 { "message": "Post id must be positive." }`
- 존재하지 않는 글: `404 { "message": "Post not found." }`
- `DRAFT` 글: `404 { "message": "Post not found." }`
- `HIDDEN` 글: `404 { "message": "Post not found." }`

## 10. 테스트 계획

모든 백엔드 테스트 메서드명은 한글 시나리오형으로 작성하고 단어 사이를 `_`로 연결한다.

### 10.1 애플리케이션 테스트

`PostDetailQueryServiceTest`를 추가한다.

필수 시나리오:

- `공개된_글_상세를_조회한다`
- `공개된_글이_없으면_조회할_수_없다`
- `조회할_글_ID가_null이면_조회할_수_없다`

순수 애플리케이션 테스트는 Spring Context를 띄우지 않는다.

### 10.2 영속성 테스트

`JpaPostDetailQueryRepositoryAdapterTest`를 추가한다.

필수 시나리오:

- `공개된_글을_ID로_조회한다`
- `임시_저장_글은_조회하지_않는다`
- `숨김_글은_조회하지_않는다`
- `태그를_이름_오름차순으로_조회한다`

JPA 테스트는 기존처럼 MySQL Testcontainers를 사용한다.

### 10.3 API 테스트

기존 `PostControllerTest`에 상세 조회 controller 시나리오를 추가한다.

필수 시나리오:

- `공개된_글_상세를_200으로_반환한다`
- `토큰_없이_공개된_글_상세를_조회할_수_있다`
- `조회할_수_없는_글이면_404를_반환한다`
- `글_ID가_유효하지_않으면_400을_반환한다`

### 10.4 통합 테스트

`PostApiIntegrationTest`에 상세 조회 세로 흐름을 추가한다.

필수 시나리오:

- `회원가입_후_작성한_공개_글을_토큰_없이_상세_조회할_수_있다`

검증:

- 회원가입 후 로그인한다.
- 발급된 토큰으로 `PUBLISHED` 글을 작성한다.
- 토큰 없이 `GET /api/posts/{postId}`를 호출한다.
- 응답이 `200 OK`다.
- 응답에 `postId`, `authorId`, `title`, `contentType`, `content`, `summary`, `tags`, `status`가 포함된다.

비공개 상태의 404 정책은 persistence 테스트와 API 테스트에서 검증한다. 통합 테스트는 공개 글 상세 조회 세로 흐름에 집중한다.

## 11. 검증 명령

백엔드 테스트:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --rerun-tasks
```

테스트명 규칙:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

예상 결과: 출력 없음.

순수 계층 annotation 침투 확인:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional|@Configuration|@Bean'
```

예상 결과: 출력 없음.

H2 재도입 확인:

```powershell
cd C:\dev\study\ddd-blog\backend
rg -n "h2database|jdbc:h2|H2Dialect|com\.h2database" .
```

예상 결과: 매치 없음. `rg`는 매치가 없으면 exit code `1`을 반환한다.

Whitespace 확인:

```powershell
cd C:\dev\study\ddd-blog
git diff --check
```

예상 결과: 출력 없음.

## 12. 성공 기준

- `GET /api/posts/{postId}`가 인증 없이 공개 글 상세를 반환한다.
- `PUBLISHED` 글만 조회된다.
- 없는 글, `DRAFT`, `HIDDEN`은 모두 `404 { "message": "Post not found." }`로 응답한다.
- 응답은 `postId`, `authorId`, `title`, `contentType`, `content`, `summary`, `tags`, `status`를 포함한다.
- 기존 `PostRepository` write port는 조회 책임을 갖지 않는다.
- 상세 조회는 read 전용 query port/service를 통해 수행된다.
- Blog 도메인과 애플리케이션 계층은 Spring/JPA annotation 없이 유지된다.
- API, 애플리케이션, 영속성, 통합 테스트가 작성된다.
- 전체 백엔드 테스트가 통과한다.
