# 글 작성 애플리케이션 서비스 1차 설계

## 1. 배경

DDD Blog는 DDD와 TDD를 학습하기 위한 블로그 프로젝트다. 현재 백엔드에는 순수 도메인 모델 1차가 구현되어 있다.

이미 구현된 도메인 요소:

- `AuthorId`
- `Post`
- `PostContent`
- `PostStatus`
- `PostSummary`
- `PostTitle`
- `TagName`

현재 `Post`는 글 생성 시점의 도메인 규칙을 검증한다. 다음 단계는 이 도메인 모델을 실제 유스케이스 흐름으로 감싸는 애플리케이션 계층을 추가하는 것이다.

이번 단계에서는 JPA, API, 인증을 붙이지 않는다. 애플리케이션 서비스가 command를 받아 도메인 객체를 만들고 repository 포트에 저장을 요청하는 흐름만 TDD로 검증한다.

## 2. 목표

이번 작업의 목표는 “글 작성 유스케이스”를 애플리케이션 계층에서 표현하는 것이다.

핵심 목표:

- `CreatePostService`로 글 작성 유스케이스를 구현한다.
- `CreatePostCommand`로 입력값을 전달한다.
- `PostRepository`를 포트로 정의한다.
- 테스트에서는 `FakePostRepository`를 사용한다.
- 저장 결과로 `PostId`를 반환한다.
- 모든 테스트는 Spring Context 없이 빠르게 실행한다.

## 3. 구현 범위

이번 작업에 포함하는 내용:

- `PostId` 값 객체 추가
- `CreatePostCommand` 추가
- `PostRepository` 포트 추가
- `CreatePostService` 추가
- 테스트 전용 `FakePostRepository` 추가
- `PostIdTest` 추가
- `CreatePostServiceTest` 추가

이번 작업에서 제외하는 내용:

- JPA entity
- Spring Data JPA repository
- MySQL 설정
- Spring `@Service`, `@Repository`
- Controller/API
- JWT 인증 연동
- 회원 존재 여부 검증
- 권한 검증
- 글 수정
- 글 삭제
- 글 조회
- 댓글 구현

## 4. 패키지 구조

도메인 계층:

```text
com.dddblog.backend.blog.domain
  AuthorId
  Post
  PostContent
  PostId
  PostStatus
  PostSummary
  PostTitle
  TagName
```

애플리케이션 계층:

```text
com.dddblog.backend.blog.application
  CreatePostCommand
  CreatePostService
  PostRepository
```

테스트 계층:

```text
com.dddblog.backend.blog.application
  CreatePostServiceTest
  FakePostRepository
```

`FakePostRepository`는 테스트 전용 구현체이므로 `src/test` 아래에 둔다.

## 5. 도메인 추가: PostId

`PostId`는 저장된 글의 식별자를 표현한다.

규칙:

- 내부 값은 `Long`이다.
- 값은 `null`일 수 없다.
- 값은 1 이상이어야 한다.
- 값이 `null`이면 `Post id must not be null.` 메시지로 실패한다.
- 값이 1보다 작으면 `Post id must be positive.` 메시지로 실패한다.
- `value()` accessor를 제공한다.
- `equals`, `hashCode`, `toString`을 구현한다.

이번 단계에서 `Post` aggregate 안에 `PostId` 필드를 추가하지 않는다. 아직 JPA 매핑과 저장 후 도메인 모델의 생명주기 정책을 정하지 않았기 때문이다.

이번 단계에서는 `PostRepository.save(Post)`의 반환값으로만 `PostId`를 사용한다.

## 6. 애플리케이션 모델

### 6.1 CreatePostCommand

`CreatePostCommand`는 글 작성 유스케이스의 입력값을 담는다.

필드:

- `Long authorId`
- `String title`
- `String content`
- `String summary`
- `List<String> tags`
- `PostStatus status`

규칙:

- command 자체에는 Bean Validation annotation을 붙이지 않는다.
- 입력값 검증과 정규화는 도메인 값 객체와 `Post` aggregate가 담당한다.
- `tags`는 `null`일 수 있으며, 이 경우 빈 태그 목록으로 처리된다.
- `summary`는 `null`일 수 있으며, 이 경우 빈 요약으로 처리된다.

### 6.2 CreatePostService

`CreatePostService`는 글 작성 유스케이스를 수행한다.

책임:

- `CreatePostCommand`를 받는다.
- command 값을 도메인 값 객체로 변환한다.
- `Post` aggregate를 생성한다.
- `PostRepository.save(post)`를 호출한다.
- 저장 결과인 `PostId`를 반환한다.

규칙:

- command는 `null`일 수 없다.
- command가 `null`이면 `IllegalArgumentException`을 던진다.
- command가 `null`이면 repository에 저장하지 않는다.

`CreatePostService`는 Spring annotation을 사용하지 않는다. 이번 단계에서는 순수 Java 객체로 생성해 테스트한다.

### 6.3 PostRepository

`PostRepository`는 애플리케이션 계층이 의존하는 저장소 포트다.

메서드:

```java
PostId save(Post post);
```

이번 단계에서는 저장만 필요하므로 조회 메서드를 추가하지 않는다.

### 6.4 FakePostRepository

`FakePostRepository`는 테스트 전용 repository 구현체다.

책임:

- `save(Post post)` 호출 시 메모리 리스트에 저장한다.
- 저장할 때 `PostId`를 순차 발급한다.
- 테스트에서 저장 여부를 확인할 수 있도록 `savedPosts()`를 제공한다.

규칙:

- 첫 번째 저장 결과는 `new PostId(1L)`이다.
- 두 번째 저장 결과는 `new PostId(2L)`이다.
- 실패 시나리오에서는 저장 목록이 비어 있어야 한다.

## 7. 저장 흐름

글 작성 유스케이스 흐름:

1. 테스트 또는 호출자가 `CreatePostCommand`를 만든다.
2. `CreatePostService.create(command)`를 호출한다.
3. 서비스는 `authorId`로 `AuthorId`를 만든다.
4. 서비스는 `title`로 `PostTitle`을 만든다.
5. 서비스는 `content`로 `PostContent`를 만든다.
6. 서비스는 `summary`로 `PostSummary`를 만든다.
7. 서비스는 `tags`를 `TagName` 목록으로 변환한다.
8. 서비스는 `Post` aggregate를 생성한다.
9. 서비스는 `PostRepository.save(post)`를 호출한다.
10. 서비스는 저장소가 반환한 `PostId`를 반환한다.

도메인 규칙 위반이 발생하면 `IllegalArgumentException`이 그대로 전파된다. 서비스는 도메인 예외를 잡아서 다른 예외로 바꾸지 않는다.

## 8. 테스트 계획

모든 테스트 메서드명은 한글 시나리오형으로 작성한다.

### 8.1 PostIdTest

필수 테스트:

- 글_ID를_생성한다
- 글_ID가_null이면_생성할_수_없다
- 글_ID가_0이면_생성할_수_없다
- 글_ID가_음수이면_생성할_수_없다

### 8.2 CreatePostServiceTest

필수 테스트:

- 유효한_요청이면_글을_저장하고_ID를_반환한다
- 저장된_글은_요청_값을_도메인_값으로_가진다
- 요약이_null이면_빈_요약으로_저장한다
- 태그_목록이_null이면_빈_태그_목록으로_저장한다
- 태그가_10개를_초과하면_저장하지_않는다
- 중복된_태그가_있으면_저장하지_않는다
- 제목이_blank이면_저장하지_않는다
- 본문이_blank이면_저장하지_않는다
- command가_null이면_저장하지_않는다

실패 시나리오에서는 `FakePostRepository.savedPosts()`가 비어 있어야 한다.

## 9. 오류 처리

이번 단계에서는 커스텀 예외를 만들지 않는다.

정책:

- 도메인 값 객체와 aggregate가 `IllegalArgumentException`을 던진다.
- `CreatePostService`는 해당 예외를 그대로 전파한다.
- API 계층이 생길 때 HTTP 응답용 예외 변환을 별도로 설계한다.

이 방식은 현재 단계의 목적이 “유스케이스와 도메인 연결”이기 때문이다.

## 10. 검증 기준

구현 완료 기준:

- `PostId` 값 객체와 테스트가 추가된다.
- `CreatePostCommand`, `CreatePostService`, `PostRepository`가 추가된다.
- `CreatePostServiceTest`가 fake repository로 글 작성 유스케이스를 검증한다.
- 도메인 테스트와 애플리케이션 서비스 테스트 모두 Spring Context 없이 실행된다.
- 전체 테스트가 `C:\java\jdk-21`로 통과한다.
- 도메인/애플리케이션 계층에 Spring, JPA annotation이 들어가지 않는다.
- 백엔드 테스트 메서드명은 한글 시나리오형을 유지한다.

검증 명령:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test
```

추가 검증:

```powershell
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table'
```

첫 번째 명령은 한글 테스트명 규칙을 확인한다. 두 번째 명령은 이번 단계에서 Spring/JPA annotation이 들어가지 않았는지 확인한다.

## 11. Git

구현은 작은 단위로 커밋한다.

권장 커밋:

- `feat: add post id value object`
- `feat: add create post service`

현재 저장소는 Git 저장소이며 작업트리는 깨끗한 상태에서 시작한다.
