# JPA 글 저장 Repository 1차 설계

## 1. 배경

DDD Blog는 DDD와 TDD를 학습하기 위한 블로그 프로젝트다. 현재 백엔드는 순수 도메인 모델과 글 작성 애플리케이션 서비스 1차가 구현되어 있다.

이미 구현된 흐름:

- `CreatePostCommand`가 글 작성 입력값을 담는다.
- `CreatePostService`가 command를 도메인 값 객체와 `Post` aggregate로 변환한다.
- `PostRepository` 애플리케이션 포트가 `save(Post)`를 제공한다.
- `PostRepository.save(post)`는 저장 결과로 `PostId`를 반환한다.

현재 `Post` aggregate는 저장 전 생성 모델로 유지되며, 내부에 `PostId`를 갖지 않는다. 이번 단계에서는 이 도메인 모델을 변경하지 않고, JPA 기반 persistence adapter를 추가해 실제 DB 저장을 검증한다.

## 2. 목표

이번 작업의 목표는 “글 작성 유스케이스의 저장 포트”를 JPA adapter로 구현하는 것이다.

핵심 목표:

- 기존 `PostRepository` 애플리케이션 포트를 유지한다.
- JPA entity와 Spring Data repository는 별도 persistence 패키지에 둔다.
- `JpaPostRepositoryAdapter`가 `PostRepository`를 구현한다.
- H2 기반 `@DataJpaTest`로 글 저장과 태그 저장을 검증한다.
- `Post` domain aggregate는 ID 없이 유지한다.
- 태그는 `tags` 테이블과 `post_tags` 조인 테이블로 정규화해 저장한다.

## 3. 구현 범위

이번 작업에 포함하는 내용:

- H2 테스트 의존성 추가
- `JpaPostEntity` 추가
- `JpaTagEntity` 추가
- `SpringDataJpaPostRepository` 추가
- `SpringDataJpaTagRepository` 추가
- `JpaPostRepositoryAdapter` 추가
- JPA repository adapter 통합 테스트 추가

이번 작업에서 제외하는 내용:

- `Post`에 `PostId` 필드 추가
- 조회 repository 메서드
- 수정 repository 메서드
- 삭제 repository 메서드
- Controller/API
- JWT와 현재 로그인 사용자 연동
- 회원 테이블과 FK 매핑
- auditing 컬럼
- soft delete
- view count
- published at
- cover image
- Flyway/Liquibase
- Testcontainers MySQL
- 태그 검색/목록 API

## 4. 패키지 구조

새 persistence adapter는 blog context 안의 별도 패키지에 둔다.

```text
com.dddblog.backend.blog.persistence
  JpaPostEntity
  JpaTagEntity
  JpaPostRepositoryAdapter
  SpringDataJpaPostRepository
  SpringDataJpaTagRepository
```

역할:

- `JpaPostEntity`: `posts` 테이블을 매핑한다.
- `JpaTagEntity`: `tags` 테이블을 매핑한다.
- `SpringDataJpaPostRepository`: `JpaPostEntity` 저장을 담당하는 Spring Data JPA repository다.
- `SpringDataJpaTagRepository`: 태그 이름 조회와 저장을 담당하는 Spring Data JPA repository다.
- `JpaPostRepositoryAdapter`: 애플리케이션 포트 `PostRepository`를 구현하고 domain `Post`를 JPA entity로 변환한다.

도메인 패키지와 애플리케이션 패키지에는 Spring/JPA annotation을 추가하지 않는다.

## 5. 테이블 매핑

이번 1차 저장에 필요한 최소 컬럼만 매핑한다.

### 5.1 posts

```text
posts
  id bigint generated
  author_id bigint not null
  title varchar(100) not null
  content_markdown text/clob not null
  summary varchar(300) not null
  status varchar not null
```

매핑 정책:

- `id`는 JPA entity의 DB 식별자다.
- `author_id`는 현재 회원 테이블과 FK로 연결하지 않고 Long 값으로 저장한다.
- `title`, `content_markdown`, `summary`, `status`는 `Post`의 값 객체에서 꺼낸 값으로 저장한다.
- `status`는 enum 문자열로 저장한다.

### 5.2 tags

```text
tags
  id bigint generated
  name varchar(30) not null unique
```

매핑 정책:

- `name`에는 `TagName`으로 정규화된 소문자 값만 저장한다.
- 이미 같은 이름의 태그가 있으면 새로 만들지 않고 재사용한다.
- DB unique constraint는 애플리케이션 로직의 방어선으로 둔다.

### 5.3 post_tags

```text
post_tags
  post_id bigint not null
  tag_id bigint not null
  unique(post_id, tag_id)
```

매핑 정책:

- `JpaPostEntity`가 `Set<JpaTagEntity>`를 갖는다.
- 조인 테이블 이름은 `post_tags`로 한다.
- 한 글 안의 중복 태그는 domain `Post`가 이미 막으므로 adapter는 별도 중복 검사를 반복하지 않는다.

## 6. 저장 흐름

`JpaPostRepositoryAdapter.save(post)` 흐름:

1. `post`가 `null`인지 확인한다.
2. `null`이면 `IllegalArgumentException`을 던진다.
3. `post.tags()`의 각 `TagName` 값으로 기존 `JpaTagEntity`를 조회한다.
4. 기존 태그가 있으면 재사용한다.
5. 기존 태그가 없으면 새 `JpaTagEntity`를 생성한다.
6. `Post` 값을 사용해 `JpaPostEntity`를 생성한다.
7. `JpaPostEntity`에 태그 set을 연결한다.
8. `SpringDataJpaPostRepository.save(entity)`를 호출한다.
9. 저장된 entity ID를 `new PostId(savedEntity.id())`로 감싸 반환한다.

JPA 저장 중 발생하는 Spring Data/JPA 예외는 별도 변환하지 않는다. API 계층이 생기면 HTTP 응답용 예외 변환을 별도로 설계한다.

## 7. 테스트 전략

이번 persistence 테스트는 H2 기반 `@DataJpaTest`를 사용한다.

필수 테스트:

- `글을_저장하면_ID를_반환한다`
- `글을_저장하면_본문_값이_posts에_저장된다`
- `이미_존재하는_태그는_새로_만들지_않고_재사용한다`
- `글이_null이면_저장할_수_없다`

테스트 정책:

- 테스트 메서드명은 한글 시나리오형으로 작성한다.
- 단어 사이는 `_`로 연결한다.
- persistence 테스트는 JPA 동작 검증이 목적이므로 Spring Context를 일부 사용하는 `@DataJpaTest`를 허용한다.
- 기존 순수 domain/application 테스트는 Spring Context 없이 유지한다.

## 8. 의존성

이미 존재하는 의존성:

- `spring-boot-starter-data-jpa`
- `mysql-connector-j`

이번 작업에서 추가할 테스트 의존성:

```kotlin
testRuntimeOnly("com.h2database:h2")
```

H2는 이번 1차 JPA 매핑 학습의 빠른 피드백을 위한 선택이다. MySQL과의 방언 차이는 이후 Testcontainers MySQL slice에서 보강한다.

## 9. 오류 처리

이번 단계에서는 커스텀 예외를 만들지 않는다.

정책:

- `JpaPostRepositoryAdapter.save(null)`은 `IllegalArgumentException`을 던진다.
- JPA/DataAccess 예외는 그대로 전파한다.
- 태그 동시 생성 충돌은 이번 1차에서 다루지 않는다.

## 10. 검증 기준

구현 완료 기준:

- `JpaPostRepositoryAdapter`가 `PostRepository`를 구현한다.
- H2 기반 repository adapter 테스트가 글 저장을 검증한다.
- 저장된 글의 주요 값이 `posts`에 저장된다.
- 이미 존재하는 태그는 새로 만들지 않고 재사용된다.
- 기존 domain/application 테스트가 계속 통과한다.
- backend 전체 테스트가 통과한다.
- 도메인/애플리케이션 패키지에는 Spring/JPA annotation이 추가되지 않는다.
- 백엔드 테스트 메서드명은 한글 시나리오형을 유지한다.

검증 명령:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test
```

추가 검증:

```powershell
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table'
```

첫 번째 추가 검증은 한글 테스트명 규칙을 확인한다. 두 번째 추가 검증은 순수 도메인/애플리케이션 계층에 Spring/JPA annotation이 들어가지 않았는지 확인한다.

## 11. Git

설계 문서 작성 후 별도 커밋한다.

권장 커밋:

- `docs: add jpa post repository design`

구현은 설계 승인 후 별도 계획을 작성한 뒤 진행한다.
