# 공개 글 목록 조회 API 설계

## 1. 배경

DDD Blog는 회원가입, 로그인/JWT 인증, 내 정보 조회, 인증된 글 작성, 공개 글 상세 조회 API까지 구현되어 있다. 이제 비회원이 공개된 글을 발견할 수 있도록 `GET /api/posts` 공개 목록 API가 필요하다.

이번 설계는 단순 목록 조회만 추가하지 않고, 글의 시간 개념도 함께 정리한다. 현재 `Post`와 `posts` 테이블에는 작성일, 수정일, 발행일이 없다. 공개 목록의 기본 정렬을 "최신 공개 글"로 정의하려면 `publishedAt`이 필요하고, 블로그 글의 생명주기 모델을 실무에 가깝게 다루려면 `createdAt`, `updatedAt`, `publishedAt`을 도메인 모델에서 명시하는 편이 낫다.

따라서 이번 slice는 하나의 설계 안에서 다음 두 작업을 다룬다.

1. `Post` 시간 모델 도입
2. 공개 글 목록 API 추가

구현과 리뷰는 두 덩어리로 나누되, 목록 API가 새 시간 모델 위에서 동작하도록 한다.

## 2. 목표

- `Post` 도메인 모델이 `createdAt`, `updatedAt`, `publishedAt`을 가진다.
- 글 생성 시 서버 유스케이스 수행 시각으로 시간 값을 결정한다.
- `PUBLISHED` 글은 `publishedAt`을 가진다.
- 이번 생성 유스케이스에서 `DRAFT`와 `HIDDEN` 글은 `publishedAt`이 없다.
- 비회원이 `GET /api/posts`로 공개 글 목록을 조회할 수 있다.
- 공개 목록은 `PUBLISHED` 글만 반환한다.
- 공개 목록은 0-based pagination을 사용한다.
- 공개 목록은 `publishedAt DESC`, 동률이면 `postId DESC`로 정렬한다.
- 목록 조회는 기존 write port인 `PostRepository`에 추가하지 않고 read 전용 query port/service로 구현한다.
- `blog.domain`과 `blog.application`은 Spring/JPA annotation 없이 순수 Java 스타일을 유지한다.

## 3. 제외 범위

- 검색어 필터
- 태그 필터
- 작성자 닉네임 필터
- 정렬 옵션
- 작성자 닉네임, 이름, 프로필 정보 join
- 대표 이미지
- 조회수
- 공개일 기반 상태 변경 API
- 글 수정 API
- 글 삭제 API
- 작성자용 내 글 목록 API
- Flyway/Liquibase migration 도입
- 프론트엔드 작업

## 4. Post 시간 모델

`Post`는 다음 시간 값을 가진다.

- `createdAt`: 글이 생성된 시각. 필수.
- `updatedAt`: 글이 마지막으로 변경된 시각. 필수.
- `publishedAt`: 글이 공개 상태가 된 시각. `PUBLISHED`일 때 필수다.

생성 규칙:

- `CreatePostService`는 현재 시각을 얻는다.
- 새 글 생성 시 `createdAt = now`, `updatedAt = now`.
- `status == PUBLISHED`이면 `publishedAt = now`.
- 새 글의 `status == DRAFT` 또는 `HIDDEN`이면 `publishedAt = null`.
- API 요청에는 `createdAt`, `updatedAt`, `publishedAt`을 받지 않는다.

시간 값은 클라이언트가 정하지 않는다. 작성, 수정, 발행 같은 유스케이스가 수행되는 서버 시각을 기준으로 한다. 이번 slice에는 수정과 상태 변경 API가 없으므로 `updatedAt`은 생성 시 `createdAt`과 같은 값으로 시작한다.

테스트 가능성을 위해 `CreatePostService`는 `Clock`을 주입받는다. Spring wiring은 `BlogApplicationConfig`에서 담당하고, 순수 애플리케이션 테스트는 고정 `Clock`을 사용한다.

## 5. 기존 API 영향

### 5.1 글 작성 API

`POST /api/posts` 요청 필드는 그대로 유지한다.

```json
{
  "title": "DDD 시작하기",
  "contentType": "MARKDOWN",
  "content": "# DDD\n\n본문",
  "summary": "DDD 소개",
  "tags": ["ddd", "tdd"],
  "status": "PUBLISHED"
}
```

응답은 기존처럼 생성된 글 ID만 반환한다.

```json
{
  "postId": 1
}
```

생성 시각은 응답하지 않지만, domain/persistence에는 저장한다.

### 5.2 공개 글 상세 조회 API

`GET /api/posts/{postId}` 응답에는 시간 필드를 추가한다.

```json
{
  "postId": 1,
  "authorId": 10,
  "title": "DDD 시작하기",
  "contentType": "MARKDOWN",
  "content": "# DDD\n\n본문",
  "summary": "DDD 소개",
  "tags": ["ddd", "tdd"],
  "status": "PUBLISHED",
  "createdAt": "2026-06-27T10:15:30Z",
  "updatedAt": "2026-06-27T10:15:30Z",
  "publishedAt": "2026-06-27T10:15:30Z"
}
```

상세 조회는 기존처럼 `PUBLISHED` 글만 공개한다. 없는 글, `DRAFT`, `HIDDEN`은 모두 `404 { "message": "Post not found." }`로 응답한다.

## 6. 공개 글 목록 API 계약

Endpoint:

```text
GET /api/posts
```

인증:

- 인증이 필요 없다.
- `SecurityConfig`에서 `GET /api/posts`는 `permitAll`로 허용한다.
- 기존 `POST /api/posts`는 계속 인증이 필요하다.

요청 파라미터:

```text
GET /api/posts?page=0&size=20
```

규칙:

- `page` 기본값은 `0`.
- `size` 기본값은 `20`.
- 최대 `size`는 `50`.
- `page < 0`이면 `400 Bad Request`.
- `size < 1` 또는 `size > 50`이면 `400 Bad Request`.

성공 응답:

```json
{
  "items": [
    {
      "postId": 10,
      "authorId": 1,
      "title": "DDD 시작하기",
      "summary": "DDD 소개",
      "tags": ["ddd", "tdd"],
      "status": "PUBLISHED",
      "createdAt": "2026-06-27T10:15:30Z",
      "updatedAt": "2026-06-27T10:15:30Z",
      "publishedAt": "2026-06-27T10:15:30Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false
}
```

응답 필드:

- `items`: 현재 페이지의 글 목록.
- `page`: 요청한 0-based page 번호.
- `size`: 요청한 page size.
- `totalElements`: 전체 공개 글 수.
- `totalPages`: 전체 page 수.
- `hasNext`: 다음 page 존재 여부.

목록 item 필드:

- `Long postId`
- `Long authorId`
- `String title`
- `String summary`
- `List<String> tags`
- `PostStatus status`
- `Instant createdAt`
- `Instant updatedAt`
- `Instant publishedAt`

목록 API는 공개 글만 반환하므로 item의 `status`는 항상 `PUBLISHED`이고 `publishedAt`은 항상 존재한다. 그래도 현재 상세 응답과 상태 표현을 맞추기 위해 `status`를 포함한다.

## 7. 애플리케이션 설계

패키지:

```text
com.dddblog.backend.blog.application
```

변경 클래스:

- `CreatePostCommand`
- `CreatePostService`
- `PostDetail`

추가 클래스:

- `PostListQuery`
- `PostListItem`
- `PostListPage`
- `PostListQueryRepository`
- `PostListQueryService`

`CreatePostService` 책임:

- null command를 방어한다.
- command primitive를 value object로 변환한다.
- 주입받은 `Clock`으로 현재 시각을 얻는다.
- `Post` 생성 시 `createdAt`, `updatedAt`, `publishedAt`을 결정한다.
- `PostRepository.save(post)`를 호출하고 `PostId`를 반환한다.

`PostListQuery` 필드:

- `int page`
- `int size`

`PostListQueryService` 책임:

- null query를 방어한다.
- `page >= 0`을 검증한다.
- `1 <= size <= 50`을 검증한다.
- `PostListQueryRepository.findPublished(query)`를 호출한다.

`PostListQueryRepository` 메서드:

```java
PostListPage findPublished(PostListQuery query);
```

순수 애플리케이션 계층 원칙을 유지하기 위해 위 클래스들에는 Spring annotation을 붙이지 않는다. Spring bean 등록은 `BlogApplicationConfig`에서 담당한다.

## 8. 도메인 설계

패키지:

```text
com.dddblog.backend.blog.domain
```

변경 클래스:

- `Post`

`Post` 생성자는 시간 값을 받는다. 이번 slice에서는 별도 `PostCreatedAt`, `PostUpdatedAt`, `PostPublishedAt` 값 객체를 만들지 않고 `Instant`를 사용한다. 시간 값 자체의 형식 검증보다 상태와의 관계가 핵심 규칙이기 때문이다.

도메인 규칙:

- `createdAt`은 null일 수 없다.
- `updatedAt`은 null일 수 없다.
- `updatedAt`은 `createdAt`보다 이전일 수 없다.
- `status == PUBLISHED`이면 `publishedAt`은 null일 수 없다.
- `publishedAt`이 있으면 `createdAt`보다 이전일 수 없다.

새 글 생성 유스케이스에서는 `DRAFT`와 `HIDDEN`의 `publishedAt`을 null로 만든다. 다만 이것을 "비공개 상태는 영원히 발행일을 가질 수 없다"는 영구 불변식으로 고정하지는 않는다. 이후 공개했다가 숨김 처리하는 상태 변경 API에서는 `HIDDEN` 글이 과거 발행 시각을 보존할지 별도로 설계한다.

이번 slice에는 수정/발행 상태 변경 메서드를 추가하지 않는다. 그 기능은 글 수정 또는 상태 변경 API slice에서 별도로 설계한다.

## 9. 영속성 설계

패키지:

```text
com.dddblog.backend.blog.persistence
```

변경 클래스:

- `JpaPostEntity`
- `JpaPostRepositoryAdapter`
- `JpaPostDetailQueryRepositoryAdapter`
- `SpringDataJpaPostRepository`

추가 클래스:

- `JpaPostListQueryRepositoryAdapter`

`posts` 테이블에 해당하는 JPA entity는 다음 컬럼을 가진다.

- `created_at`
- `updated_at`
- `published_at`

현재 프로젝트에는 Flyway/Liquibase가 없고 테스트는 Hibernate `ddl-auto=create-drop`에 의존한다. 따라서 이번 slice는 JPA entity mapping과 테스트 기대값을 변경하되 migration 파일은 만들지 않는다.

목록 조회 조건:

- `status = PUBLISHED`
- 정렬: `publishedAt DESC`, `id DESC`
- pagination 적용
- tags는 item별 이름 오름차순

구현은 Spring Data `PageRequest`와 `Sort`를 사용할 수 있다. 목록 조회용 adapter는 `PostListQueryRepository`를 구현하고, `JpaPostEntity`를 `PostListItem`으로 변환한다.

N+1 문제는 이번 첫 slice에서 대량 최적화 대상으로 삼지 않는다. 다만 persistence 테스트에서 tag mapping과 정렬은 검증한다. 추후 목록 성능 개선 slice에서 fetch join, entity graph, projection, batch size 등을 별도로 검토한다.

## 10. API 계층 설계

패키지:

```text
com.dddblog.backend.blog.api
```

변경 클래스:

- `PostDetailResponse`
- `PostDetailApiService`
- `PostController`

추가 클래스:

- `PostListItemResponse`
- `PostListResponse`
- `PostListApiService`

`PostListApiService` 책임:

- nullable request parameter에 기본값을 적용한다.
- `PostListQuery`를 생성한다.
- `PostListQueryService`를 호출한다.
- `PostListPage`를 `PostListResponse`로 변환한다.

`PostController` 책임:

- `GET /api/posts` 요청을 받는다.
- 인증 principal을 요구하지 않는다.
- `PostListApiService.getList(page, size)`에 위임한다.
- 성공 시 `200 OK`와 `PostListResponse`를 반환한다.

오류 응답 shape는 기존과 같이 `{ "message": "..." }`를 사용한다.

대표 실패:

- `page < 0`: `400 { "message": "Page must be zero or positive." }`
- `size < 1`: `400 { "message": "Page size must be at least 1." }`
- `size > 50`: `400 { "message": "Page size must be 50 or less." }`

## 11. 보안 설계

`SecurityConfig`는 다음 요청을 인증 없이 허용한다.

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/posts`
- `GET /api/posts/{postId}`

그 외 요청은 기존처럼 인증이 필요하다. 특히 `POST /api/posts`는 계속 JWT Bearer 인증을 요구한다.

## 12. 테스트 계획

모든 백엔드 테스트 메서드명은 한글 시나리오형으로 작성하고 단어 사이를 `_`로 연결한다.

### 12.1 도메인 테스트

`PostTest`에 시간 규칙 시나리오를 추가한다.

- `글을_생성하면_작성일과_수정일을_가진다`
- `공개_글을_생성하면_발행일을_가진다`
- `임시_저장_글은_발행일을_가질_수_없다`
- `숨김_글은_발행일을_가질_수_없다`
- `작성일이_null이면_생성할_수_없다`
- `수정일이_null이면_생성할_수_없다`
- `수정일이_작성일보다_이전이면_생성할_수_없다`
- `공개_글의_발행일이_null이면_생성할_수_없다`
- `발행일이_작성일보다_이전이면_생성할_수_없다`

### 12.2 글 작성 애플리케이션 테스트

`CreatePostServiceTest`에 Clock 기반 생성 시각 시나리오를 추가한다.

- `글을_생성하면_현재_시각을_작성일과_수정일로_저장한다`
- `공개_글을_생성하면_현재_시각을_발행일로_저장한다`
- `임시_저장_글을_생성하면_발행일을_저장하지_않는다`

순수 애플리케이션 테스트는 Spring Context를 띄우지 않는다.

### 12.3 공개 목록 애플리케이션 테스트

`PostListQueryServiceTest`를 추가한다.

- `공개_글_목록을_조회한다`
- `조회_조건이_null이면_조회할_수_없다`
- `페이지가_음수이면_조회할_수_없다`
- `페이지_크기가_1보다_작으면_조회할_수_없다`
- `페이지_크기가_50보다_크면_조회할_수_없다`

### 12.4 영속성 테스트

기존 `JpaPostRepositoryAdapterTest`와 `JpaPostDetailQueryRepositoryAdapterTest`를 시간 필드에 맞게 확장한다.

`JpaPostListQueryRepositoryAdapterTest`를 추가한다.

- `공개된_글_목록을_조회한다`
- `임시_저장_글은_목록에_포함하지_않는다`
- `숨김_글은_목록에_포함하지_않는다`
- `발행일_내림차순으로_조회한다`
- `발행일이_같으면_글_ID_내림차순으로_조회한다`
- `페이지와_크기를_적용해_조회한다`
- `태그를_이름_오름차순으로_조회한다`

JPA 테스트는 기존처럼 MySQL Testcontainers를 사용한다.

### 12.5 API 테스트

`PostDetailApiServiceTest`와 `PostControllerTest`를 시간 필드에 맞게 확장한다.

`PostListApiServiceTest`를 추가한다.

- `공개_글_목록_응답으로_변환한다`
- `페이지와_크기가_null이면_기본값으로_조회한다`

`PostControllerTest`에 목록 controller 시나리오를 추가한다.

- `공개_글_목록을_200으로_반환한다`
- `토큰_없이_공개_글_목록을_조회할_수_있다`
- `페이지가_유효하지_않으면_400을_반환한다`
- `페이지_크기가_유효하지_않으면_400을_반환한다`

### 12.6 통합 테스트

`PostApiIntegrationTest`에 공개 목록 세로 흐름을 추가한다.

- `회원가입_후_작성한_공개_글을_토큰_없이_목록에서_조회할_수_있다`

검증:

- 회원가입 후 로그인한다.
- 발급된 토큰으로 여러 글을 작성한다.
- `PUBLISHED`, `DRAFT`, `HIDDEN`을 섞어 작성한다.
- 토큰 없이 `GET /api/posts`를 호출한다.
- 응답은 `200 OK`다.
- 응답에는 `PUBLISHED` 글만 포함된다.
- 응답 item은 `postId`, `authorId`, `title`, `summary`, `tags`, `status`, `createdAt`, `updatedAt`, `publishedAt`을 포함한다.
- 목록은 `publishedAt DESC`, 동률이면 `postId DESC`로 정렬된다.

## 13. 검증 명령

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

## 14. 성공 기준

- `Post`가 `createdAt`, `updatedAt`, `publishedAt`을 가진다.
- `CreatePostService`가 Clock 기반으로 생성 시각을 결정한다.
- `PUBLISHED` 글은 `publishedAt`을 가지고, 이번 생성 유스케이스에서 `DRAFT`/`HIDDEN` 글은 `publishedAt`이 없다.
- `posts` JPA entity가 `created_at`, `updated_at`, `published_at`을 저장한다.
- 공개 상세 조회 응답이 시간 필드를 포함한다.
- `GET /api/posts`가 인증 없이 공개 글 목록을 반환한다.
- 목록은 `PUBLISHED` 글만 반환한다.
- 목록은 `publishedAt DESC`, 동률이면 `postId DESC`로 정렬된다.
- 목록은 0-based pagination과 최대 page size 50 규칙을 지킨다.
- 기존 `PostRepository` write port는 조회 책임을 갖지 않는다.
- 목록 조회는 read 전용 query port/service를 통해 수행된다.
- Blog 도메인과 애플리케이션 계층은 Spring/JPA annotation 없이 유지된다.
- API, 애플리케이션, 영속성, 통합 테스트가 작성된다.
- 전체 백엔드 테스트가 통과한다.
