# 회원 저장소 영속성 1차와 회원 ID 생성 방식 설계

## 1. 배경

DDD Blog는 DDD와 TDD를 학습하기 위한 블로그 프로젝트다. 현재 백엔드에는 글 도메인, 글 작성 애플리케이션 서비스, 글 JPA 저장소 adapter, 회원 도메인, 회원가입 애플리케이션 서비스가 구현되어 있다.

회원 도메인은 `Member` 생성 시점에 `MemberId`를 필수로 요구한다. 회원가입 애플리케이션 서비스는 현재 `MemberRepository.nextId()`로 저장 전에 ID를 발급받고, 그 ID로 `Member.register(...)`를 호출한다.

다음 단계는 회원 저장소를 JPA adapter로 구현하는 것이다. 이때 MySQL/JPA auto-increment는 보통 저장 후 ID를 생성하므로, 현재 도메인 규칙과 바로 맞지 않는다. 이번 설계에서는 도메인의 "ID 필수" 규칙을 유지하되, ID 생성 책임을 저장소에서 분리해 명확한 application port로 만든다.

또한 이번 단계에서 JPA repository 테스트 환경을 MySQL Testcontainers로 통일한다. 기존 글 저장소 테스트는 H2 기반으로 작성되어 있지만, 운영 대상 DB가 MySQL이므로 JPA mapping과 DB 제약을 MySQL 위에서 검증하도록 정리한다.

## 2. 목표

이번 작업의 목표는 "회원 저장소 영속성 1차"와 "회원 ID 생성 방식 확정"이다.

핵심 목표:

- `MemberRepository.nextId()`를 제거한다.
- 회원 ID 생성 책임을 `MemberIdGenerator` application port로 분리한다.
- `RegisterMemberService`가 `MemberIdGenerator.nextId()`로 저장 전 ID를 발급받는다.
- 회원 저장소 JPA adapter를 추가한다.
- 회원 ID 생성기는 DB 테이블 기반으로 구현한다.
- `members.login_id`, `members.nickname`에 unique 제약을 둔다.
- 회원 persistence 테스트는 MySQL Testcontainers에서 실행한다.
- 기존 글 persistence 테스트도 MySQL Testcontainers로 전환한다.
- Flyway/Liquibase 없이 Hibernate DDL로 테스트 schema를 생성한다.

## 3. 구현 범위

이번 작업에 포함하는 내용:

- `MemberIdGenerator` application port 추가
- `RegisterMemberService` 생성자와 ID 생성 흐름 변경
- `MemberRepository`에서 `nextId()` 제거
- `member.persistence` 패키지 추가
- `JpaMemberEntity` 추가
- `SpringDataJpaMemberRepository` 추가
- `JpaMemberRepositoryAdapter` 추가
- `JpaMemberIdSequenceEntity` 추가
- `SpringDataJpaMemberIdSequenceRepository` 추가
- `JpaMemberIdGenerator` 추가
- Testcontainers MySQL 테스트 설정 추가
- 회원 persistence 테스트 추가
- 회원 ID 생성기 테스트 추가
- 기존 `JpaPostRepositoryAdapterTest`를 MySQL Testcontainers 기반으로 전환

이번 작업에서 제외하는 내용:

- 회원가입 REST API
- `RegisterMemberService` Spring bean 등록
- BCrypt password encoder
- 평문 비밀번호 처리
- 로그인
- JWT 인증
- refresh token
- Flyway/Liquibase migration
- 회원 조회 repository method
- 회원 수정 repository method
- 회원 삭제 repository method
- `posts.author_id`와 `members.id` FK 연결
- DB unique violation의 application 예외 변환
- 커스텀 예외 계층과 API error response

## 4. 애플리케이션 경계

### 4.1 MemberIdGenerator

`MemberIdGenerator`는 회원 ID 생성 책임을 표현하는 application port다.

패키지:

```text
com.dddblog.backend.member.application
```

예상 형태:

```java
public interface MemberIdGenerator {

	MemberId nextId();
}
```

`MemberRepository`는 저장소 책임만 가진다. 기존 `nextId()`는 제거한다.

예상 형태:

```java
public interface MemberRepository {

	boolean existsByLoginId(LoginId loginId);

	boolean existsByNickname(Nickname nickname);

	MemberId save(Member member);
}
```

### 4.2 RegisterMemberService

`RegisterMemberService`는 `MemberRepository`와 `MemberIdGenerator`를 생성자로 받는다.

예상 흐름:

```text
RegisterMemberService.register(command)
→ command null 검증
→ MemberName, Nickname, LoginId, PasswordHash 생성
→ loginId 중복 검사
→ nickname 중복 검사
→ memberIdGenerator.nextId()
→ Member.register(...)
→ memberRepository.save(member)
→ 저장된 MemberId 반환
```

`Member` 도메인 모델은 그대로 `MemberId` 필수 규칙을 유지한다. `member.application`과 `member.domain`에는 Spring/JPA annotation을 붙이지 않는다.

## 5. 회원 Persistence 구성

회원 persistence 패키지는 기존 blog persistence adapter 패턴에 맞춘다.

패키지:

```text
com.dddblog.backend.member.persistence
```

구성요소:

```text
JpaMemberEntity
SpringDataJpaMemberRepository
JpaMemberRepositoryAdapter
JpaMemberIdSequenceEntity
SpringDataJpaMemberIdSequenceRepository
JpaMemberIdGenerator
```

### 5.1 members mapping

`JpaMemberEntity`는 `members` 테이블에 매핑한다.

컬럼:

```text
id             BIGINT primary key
name           VARCHAR
nickname       VARCHAR unique
login_id       VARCHAR unique
password_hash  VARCHAR
role           VARCHAR
status         VARCHAR
```

`role`은 `MemberRole`을 문자열로 저장한다. `status`는 `MemberStatus`를 문자열로 저장한다.

`login_id`와 `nickname`은 application service에서 중복 선검사를 하더라도 DB unique 제약을 둔다. 이는 동시 가입 요청에서 최종 방어선 역할을 한다.

### 5.2 JpaMemberRepositoryAdapter

`JpaMemberRepositoryAdapter`는 application port인 `MemberRepository`를 구현한다.

메서드:

```text
existsByLoginId(LoginId loginId)
existsByNickname(Nickname nickname)
save(Member member)
```

`save(member)`는 `member`가 `null`이면 `IllegalArgumentException`을 던진다.

예상 메시지:

```text
Member must not be null.
```

`save(member)`는 domain `Member`를 `JpaMemberEntity`로 변환해 저장하고, 저장된 `MemberId`를 반환한다.

이번 범위에서는 저장된 entity를 다시 domain `Member`로 복원하는 조회 method를 만들지 않는다.

## 6. DB 테이블 기반 ID 생성기

`JpaMemberIdGenerator`는 `MemberIdGenerator` port를 구현한다.

ID 생성기는 별도 테이블을 사용한다.

예상 테이블:

```text
member_id_sequences
```

예상 컬럼:

```text
name        VARCHAR primary key
next_value  BIGINT
```

회원 ID용 row는 하나만 사용한다.

예상 row:

```text
name = 'member'
next_value = 1
```

`nextId()`는 현재 `next_value`를 `MemberId`로 반환하고, 다음 호출을 위해 값을 증가시킨다. 반환되는 값은 항상 양수여야 한다.

`JpaMemberIdGenerator`는 회원 ID row를 비관적 쓰기 lock으로 조회한다. Spring Data JPA repository에는 `@Lock(LockModeType.PESSIMISTIC_WRITE)`를 사용한 조회 method를 둔다. row가 없으면 첫 호출에서 `name = 'member'`, `next_value = 2`인 row를 만들고 `new MemberId(1L)`을 반환한다.

동시 첫 호출에서 발생할 수 있는 초기 row 생성 경합은 이번 1차 범위의 필수 테스트로 두지 않는다. row가 존재한 뒤의 연속 ID 발급 흐름은 lock이 걸린 row 갱신으로 처리한다.

## 7. Testcontainers MySQL 테스트 통일

이번 작업부터 JPA repository 테스트는 MySQL Testcontainers를 사용한다.

Gradle test dependency:

```kotlin
testImplementation("org.testcontainers:junit-jupiter")
testImplementation("org.testcontainers:mysql")
```

테스트는 `@DataJpaTest` 스타일을 유지하되, datasource는 MySQL 컨테이너가 제공한다. schema는 Flyway/Liquibase 없이 Hibernate DDL로 생성한다.

신규 테스트:

```text
JpaMemberRepositoryAdapterTest
JpaMemberIdGeneratorTest
```

개편 테스트:

```text
JpaPostRepositoryAdapterTest
```

기존 글 저장소 테스트의 시나리오는 유지한다. 테스트 DB만 H2에서 MySQL Testcontainers로 바꾼다.

기존 글 persistence 테스트까지 MySQL Testcontainers로 전환한 뒤에는 H2 기반 JPA 테스트가 남지 않는다. 따라서 `testRuntimeOnly("com.h2database:h2")` 의존성은 제거한다.

## 8. 테스트 계획

모든 백엔드 테스트 메서드명은 한글 시나리오형으로 작성한다. 단어 사이는 `_`로 연결한다.

### 8.1 회원 저장소 테스트

`JpaMemberRepositoryAdapterTest` 필수 시나리오:

- `회원을_저장하면_ID를_반환한다`
- `회원을_저장하면_members에_도메인_값이_저장된다`
- `저장된_로그인_ID가_있으면_존재한다고_판단한다`
- `저장된_닉네임이_있으면_존재한다고_판단한다`
- `로그인_ID는_중복_저장할_수_없다`
- `닉네임은_중복_저장할_수_없다`
- `회원이_null이면_저장할_수_없다`

Unique 제약 테스트는 DB 제약이 실제로 동작하는지만 확인한다. 어떤 unique 제약이 깨졌는지 판별해 application 예외로 바꾸는 처리는 이번 범위에서 제외한다.

### 8.2 회원 ID 생성기 테스트

`JpaMemberIdGeneratorTest` 필수 시나리오:

- `ID를_발급하면_양수_ID를_반환한다`
- `ID를_연속으로_발급하면_서로_다른_ID를_반환한다`

동시성 테스트는 이번 1차 범위에서 제외한다. 단일 row 갱신 방식과 transaction 경계가 정리된 뒤 별도 설계에서 다룬다.

### 8.3 기존 글 저장소 테스트

`JpaPostRepositoryAdapterTest`의 기존 시나리오는 유지한다.

기존 시나리오:

- `글을_저장하면_ID를_반환한다`
- `글을_저장하면_본문_값이_posts에_저장된다`
- `이미_존재하는_태그는_새로_만들지_않고_재사용한다`
- `글이_null이면_저장할_수_없다`

변경 목적은 테스트 DB를 MySQL Testcontainers로 통일하는 것이다. 글 저장소 기능 자체를 변경하지 않는다.

## 9. 오류 처리

기존 애플리케이션 오류 처리 스타일을 유지한다.

`RegisterMemberService`:

- command null: `Register member command must not be null.`
- 로그인 ID 중복 선검사: `Login id already exists.`
- 닉네임 중복 선검사: `Nickname already exists.`
- 값 객체 검증 실패: 기존 value object 예외 그대로 통과

`JpaMemberRepositoryAdapter`:

- member null: `Member must not be null.`

DB unique 제약 위반은 persistence 테스트에서 검증한다. 이번 단계에서는 unique violation을 잡아 application exception으로 변환하지 않는다. 동시 가입 race에서 발생할 수 있는 DB 예외 변환은 회원가입 API와 transaction boundary 설계 때 다룬다.

## 10. 검증 기준

구현 완료 기준:

- `MemberIdGenerator` application port가 추가된다.
- `RegisterMemberService`는 `MemberIdGenerator`를 통해 ID를 발급받는다.
- `MemberRepository`는 저장소 기능만 가진다.
- `member.domain`과 `member.application`은 Spring/JPA annotation 없이 유지된다.
- 회원 persistence adapter는 `member.persistence`에만 둔다.
- `members.login_id`, `members.nickname`에 unique 제약이 있다.
- 회원 ID 생성기는 DB 테이블 기반으로 동작한다.
- 신규 회원 persistence 테스트가 MySQL Testcontainers에서 통과한다.
- 기존 글 persistence 테스트도 MySQL Testcontainers에서 통과한다.
- Flyway/Liquibase, API, BCrypt, JWT는 추가하지 않는다.
- 전체 백엔드 테스트가 `C:\java\jdk-21`로 통과한다.
- 백엔드 테스트 메서드명은 한글 시나리오형을 유지한다.

검증 명령:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test
```

테스트명 규칙 확인:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

순수 domain/application annotation 확인:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional'
```

위 두 규칙 확인 명령은 출력이 없어야 한다.

## 11. Git

설계 문서 커밋:

```text
docs: add member persistence id generation design
```

구현 단계는 별도 계획 문서 승인 후 진행한다. 구현 전에는 격리된 worktree를 사용한다.
