# 회원 도메인 1차 설계

## 1. 배경

DDD Blog는 DDD와 TDD를 학습하기 위한 블로그 프로젝트다. 현재 백엔드에는 글 작성 도메인, 글 작성 애플리케이션 서비스, JPA 글 저장소 adapter가 구현되어 있다.

현재 `Post`는 `AuthorId`를 통해 작성자를 참조하지만, 실제 회원 aggregate는 아직 없다. 글 작성 API를 붙이기 전에 회원 도메인의 최소 골격을 먼저 만들면 이후 회원가입, 로그인, JWT 인증, 글 작성자 연결 흐름을 더 자연스럽게 확장할 수 있다.

이번 단계에서는 Spring, JPA, API를 붙이지 않는다. 순수 Java 도메인 객체와 JUnit 테스트만으로 회원 도메인의 첫 번째 조각을 구현한다.

## 2. 목표

이번 작업의 목표는 “회원”을 순수 도메인 모델로 표현하는 것이다.

핵심 목표:

- `Member` aggregate를 추가한다.
- 회원 식별자와 프로필 값을 값 객체로 분리한다.
- 회원은 평문 비밀번호가 아니라 `PasswordHash`만 가진다.
- 신규 회원의 기본 권한과 상태를 도메인에서 표현한다.
- 모든 테스트는 Spring Context 없이 빠르게 실행한다.

## 3. 구현 범위

이번 작업에 포함하는 내용:

- `MemberId` 값 객체 추가
- `MemberName` 값 객체 추가
- `Nickname` 값 객체 추가
- `LoginId` 값 객체 추가
- `PasswordHash` 값 객체 추가
- `MemberRole` enum 추가
- `MemberStatus` enum 추가
- `Member` aggregate 추가
- 새 도메인 규칙에 대한 단위 테스트 추가

이번 작업에서 제외하는 내용:

- 회원가입 애플리케이션 서비스
- `MemberRepository` 포트
- JPA entity
- Spring Data JPA repository
- REST API
- BCrypt 해시 생성과 비밀번호 검증
- 로그인
- JWT 인증
- 로그인 ID 중복 검사
- 닉네임 중복 검사
- 회원 정보 수정
- 회원 탈퇴
- Post persistence의 `author_id` FK 연결

중복 검사처럼 저장소 조회가 필요한 규칙은 애플리케이션 서비스 또는 도메인 서비스 설계 단계에서 다룬다.

## 4. 패키지 구조

회원 도메인은 Blog 도메인과 분리해 다음 패키지에 둔다.

```text
com.dddblog.backend.member.domain
  Member
  MemberId
  MemberName
  Nickname
  LoginId
  PasswordHash
  MemberRole
  MemberStatus
```

테스트는 같은 경계의 테스트 패키지에 둔다.

```text
com.dddblog.backend.member.domain
  MemberIdTest
  MemberNameTest
  NicknameTest
  LoginIdTest
  PasswordHashTest
  MemberTest
```

`blog.domain.AuthorId`는 이번 단계에서 변경하지 않는다. 이후 글 작성 유스케이스에서 `AuthorId`는 작성자인 회원의 ID를 참조하는 값 객체로 유지한다.

## 5. 도메인 모델

### 5.1 Member

`Member`는 블로그 서비스 회원 하나를 표현하는 aggregate다. 이번 단계에서는 회원 생성 시점의 필수 값과 기본 상태를 책임진다.

필드:

- `MemberId id`
- `MemberName name`
- `Nickname nickname`
- `LoginId loginId`
- `PasswordHash passwordHash`
- `MemberRole role`
- `MemberStatus status`

규칙:

- 회원은 ID 없이 생성될 수 없다.
- 회원은 이름 없이 생성될 수 없다.
- 회원은 닉네임 없이 생성될 수 없다.
- 회원은 로그인 ID 없이 생성될 수 없다.
- 회원은 비밀번호 해시 없이 생성될 수 없다.
- 회원은 권한 없이 생성될 수 없다.
- 회원은 상태 없이 생성될 수 없다.
- 신규 회원은 기본 권한 `MEMBER`와 기본 상태 `ACTIVE`를 가진다.

생성 편의 메서드:

```java
public static Member register(
	MemberId id,
	MemberName name,
	Nickname nickname,
	LoginId loginId,
	PasswordHash passwordHash
)
```

`register`는 신규 회원 기본값을 적용한다. 테스트에서 명시적인 상태를 검증할 수 있도록 주 생성자는 모든 필드를 받는 형태로 둔다.

### 5.2 MemberId

`MemberId`는 회원 식별자를 표현한다.

규칙:

- 내부 값은 `Long`이다.
- 값은 `null`일 수 없다.
- 값은 1 이상이어야 한다.
- `value()` accessor를 제공한다.
- `equals`, `hashCode`, `toString`을 구현한다.

### 5.3 MemberName

`MemberName`은 회원의 실제 이름을 표현한다.

규칙:

- 이름은 `null`일 수 없다.
- 이름은 blank일 수 없다.
- 이름은 앞뒤 공백을 제거한다.
- 정규화 후 이름은 1자 이상 30자 이하여야 한다.

### 5.4 Nickname

`Nickname`은 서비스에서 표시되는 회원 닉네임을 표현한다.

규칙:

- 닉네임은 `null`일 수 없다.
- 닉네임은 blank일 수 없다.
- 닉네임은 앞뒤 공백을 제거한다.
- 정규화 후 닉네임은 2자 이상 20자 이하여야 한다.

닉네임 중복 검사는 이번 단계에서 제외한다.

### 5.5 LoginId

`LoginId`는 로그인에 사용하는 회원의 ID 문자열을 표현한다.

규칙:

- 로그인 ID는 `null`일 수 없다.
- 로그인 ID는 blank일 수 없다.
- 로그인 ID는 앞뒤 공백을 제거한다.
- 정규화 후 로그인 ID는 4자 이상 30자 이하여야 한다.

로그인 ID 중복 검사는 이번 단계에서 제외한다.

### 5.6 PasswordHash

`PasswordHash`는 해시 처리된 비밀번호 문자열을 표현한다.

규칙:

- 비밀번호 해시는 `null`일 수 없다.
- 비밀번호 해시는 blank일 수 없다.
- 비밀번호 해시는 입력값 그대로 보관한다.

이번 단계에서는 평문 `Password` 값 객체를 만들지 않는다. 평문 비밀번호 길이 검증, BCrypt 해시 생성, 비밀번호 일치 검증은 회원가입과 로그인 애플리케이션 서비스를 설계할 때 다룬다.

### 5.7 MemberRole

`MemberRole`은 회원 권한을 표현한다.

상태:

- `MEMBER`: 일반 회원
- `ADMIN`: 관리자

초기 구현에서는 신규 회원이 `MEMBER` 권한을 가진다는 규칙만 검증한다. 관리자 권한 부여 기능은 제외한다.

### 5.8 MemberStatus

`MemberStatus`는 회원 계정 상태를 표현한다.

상태:

- `ACTIVE`: 활성 회원
- `INACTIVE`: 비활성 회원

초기 구현에서는 신규 회원이 `ACTIVE` 상태를 가진다는 규칙만 검증한다. 탈퇴, 정지, 비활성화 유스케이스는 제외한다.

## 6. 테스트 계획

모든 테스트 메서드명은 한글 시나리오형으로 작성한다.

필수 테스트:

- `MemberIdTest`
  - 회원_ID를_생성한다
  - 회원_ID가_null이면_생성할_수_없다
  - 회원_ID가_0이면_생성할_수_없다
  - 회원_ID가_음수이면_생성할_수_없다
- `MemberNameTest`
  - 회원_이름을_생성한다
  - 이름은_앞뒤_공백을_제거한다
  - 이름이_null이면_생성할_수_없다
  - 이름이_blank이면_생성할_수_없다
  - 이름이_30자를_초과하면_생성할_수_없다
- `NicknameTest`
  - 닉네임을_생성한다
  - 닉네임은_앞뒤_공백을_제거한다
  - 닉네임이_null이면_생성할_수_없다
  - 닉네임이_blank이면_생성할_수_없다
  - 닉네임이_2자보다_짧으면_생성할_수_없다
  - 닉네임이_20자를_초과하면_생성할_수_없다
- `LoginIdTest`
  - 로그인_ID를_생성한다
  - 로그인_ID는_앞뒤_공백을_제거한다
  - 로그인_ID가_null이면_생성할_수_없다
  - 로그인_ID가_blank이면_생성할_수_없다
  - 로그인_ID가_4자보다_짧으면_생성할_수_없다
  - 로그인_ID가_30자를_초과하면_생성할_수_없다
- `PasswordHashTest`
  - 비밀번호_해시를_생성한다
  - 비밀번호_해시가_null이면_생성할_수_없다
  - 비밀번호_해시가_blank이면_생성할_수_없다
- `MemberTest`
  - 신규_회원을_등록한다
  - 신규_회원은_MEMBER_권한을_가진다
  - 신규_회원은_ACTIVE_상태를_가진다
  - ID가_없으면_회원을_생성할_수_없다
  - 이름이_없으면_회원을_생성할_수_없다
  - 닉네임이_없으면_회원을_생성할_수_없다
  - 로그인_ID가_없으면_회원을_생성할_수_없다
  - 비밀번호_해시가_없으면_회원을_생성할_수_없다
  - 권한이_없으면_회원을_생성할_수_없다
  - 상태가_없으면_회원을_생성할_수_없다

## 7. 오류 처리

이번 단계의 도메인 모델은 잘못된 입력에 대해 `IllegalArgumentException`을 던진다.

커스텀 도메인 예외와 에러 코드는 애플리케이션/API 계층을 설계할 때 도입 여부를 결정한다. 이번 단계에서는 도메인 규칙 검증에 집중하기 위해 예외 체계를 단순하게 유지한다.

## 8. 검증 기준

구현 완료 기준:

- 회원 도메인 클래스와 테스트가 `member.domain` 패키지에 추가된다.
- 도메인 클래스에는 Spring/JPA annotation이 들어가지 않는다.
- 회원 도메인 테스트는 Spring Context 없이 실행된다.
- 기존 Blog 도메인과 persistence 코드는 변경하지 않는다.
- 전체 백엔드 테스트가 `C:\java\jdk-21`로 통과한다.
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
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\member\domain -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional'
```

첫 번째 명령은 한글 테스트명 규칙을 확인한다. 두 번째 명령은 회원 순수 도메인에 Spring/JPA annotation이 들어가지 않았는지 확인한다.

## 9. Git

설계 문서 커밋:

- `docs: add member domain design`

구현 단계는 별도 계획 문서 승인 후 작은 단위로 커밋한다.
