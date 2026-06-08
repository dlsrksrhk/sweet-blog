# 회원가입 애플리케이션 서비스 1차 설계

## 1. 배경

DDD Blog는 DDD와 TDD를 학습하기 위한 블로그 프로젝트다. 현재 백엔드에는 글 작성 도메인, 글 작성 애플리케이션 서비스, 글 JPA 저장소 adapter, 회원 도메인 모델이 구현되어 있다.

회원 도메인은 `Member`, `MemberId`, `MemberName`, `Nickname`, `LoginId`, `PasswordHash`, `MemberRole`, `MemberStatus`를 통해 회원 생성 시점의 순수 도메인 규칙을 표현한다. 다음 단계는 이 도메인 모델을 회원가입 유스케이스로 감싸는 애플리케이션 계층을 추가하는 것이다.

이번 단계에서는 Spring, JPA, API, BCrypt, JWT를 붙이지 않는다. 순수 Java 애플리케이션 서비스와 repository port, fake repository 테스트만으로 회원가입 흐름의 첫 번째 조각을 구현한다.

## 2. 목표

이번 작업의 목표는 “회원가입” 유스케이스를 애플리케이션 서비스로 표현하는 것이다.

핵심 목표:

- `member.application` 패키지를 추가한다.
- 회원가입 요청 값을 담는 command를 추가한다.
- 회원 저장소를 추상화하는 application port를 추가한다.
- 회원가입 서비스가 command primitive 값을 도메인 값 객체로 변환한다.
- 회원가입 서비스가 로그인 ID와 닉네임 중복을 저장 전에 검사한다.
- 회원 ID 생성 책임은 repository port에 둔다.
- 회원가입 서비스는 저장된 `MemberId`를 반환한다.
- 모든 테스트는 Spring Context 없이 빠르게 실행한다.

## 3. 구현 범위

이번 작업에 포함하는 내용:

- `RegisterMemberCommand` 추가
- `RegisterMemberService` 추가
- `MemberRepository` application port 추가
- 테스트용 `FakeMemberRepository` 추가
- 회원가입 애플리케이션 서비스 단위 테스트 추가

이번 작업에서 제외하는 내용:

- 회원 JPA entity
- Spring Data JPA repository
- REST API
- Spring bean 등록과 configuration
- BCrypt 해시 생성
- 평문 비밀번호 검증
- 로그인
- JWT 인증
- refresh token
- 회원 정보 수정
- 회원 탈퇴
- Post persistence의 `author_id` FK 연결
- 커스텀 예외 계층과 에러 코드

`RegisterMemberCommand`는 이미 해시 처리된 비밀번호 문자열인 `passwordHash`를 받는다. 평문 비밀번호를 받아 해시로 변환하는 흐름은 이후 API 또는 인증 설계 단계에서 다룬다.

## 4. 패키지 구조

회원가입 애플리케이션 계층은 회원 도메인과 같은 bounded context 아래에 둔다.

```text
com.dddblog.backend.member.application
  RegisterMemberCommand
  RegisterMemberService
  MemberRepository
```

테스트는 같은 경계의 테스트 패키지에 둔다.

```text
com.dddblog.backend.member.application
  RegisterMemberServiceTest
  FakeMemberRepository
```

`RegisterMemberService`는 순수 Java class로 둔다. 이번 단계에서는 `@Service`, `@Transactional` 같은 Spring annotation을 붙이지 않는다.

## 5. 애플리케이션 모델

### 5.1 RegisterMemberCommand

`RegisterMemberCommand`는 회원가입 유스케이스의 입력 값을 담는 record다.

필드:

- `String name`
- `String nickname`
- `String loginId`
- `String passwordHash`

`memberId`는 command에 포함하지 않는다. 회원 ID는 repository port가 생성한다.

예상 형태:

```java
public record RegisterMemberCommand(
	String name,
	String nickname,
	String loginId,
	String passwordHash
) {
}
```

### 5.2 MemberRepository

`MemberRepository`는 회원가입 서비스가 필요로 하는 저장소 기능을 표현하는 application port다.

메서드:

- `boolean existsByLoginId(LoginId loginId)`
- `boolean existsByNickname(Nickname nickname)`
- `MemberId nextId()`
- `MemberId save(Member member)`

예상 형태:

```java
public interface MemberRepository {

	boolean existsByLoginId(LoginId loginId);

	boolean existsByNickname(Nickname nickname);

	MemberId nextId();

	MemberId save(Member member);
}
```

`nextId()`는 repository가 회원 ID 생성 책임을 가진다는 의도를 드러낸다. 현재 `Member` aggregate는 ID 없이 생성될 수 없으므로, 서비스는 저장 전에 `nextId()`로 ID를 받은 뒤 `Member.register(...)`를 호출한다.

`nextId()`는 로그인 ID와 닉네임 중복 검사를 통과한 뒤 호출한다. 실패할 요청이 불필요하게 ID를 소비하지 않게 하기 위해서다.

### 5.3 RegisterMemberService

`RegisterMemberService`는 회원가입 유스케이스를 조율한다.

생성자:

- `RegisterMemberService(MemberRepository memberRepository)`

메서드:

- `MemberId register(RegisterMemberCommand command)`

흐름:

1. command가 `null`이면 거부한다.
2. `name`, `nickname`, `loginId`, `passwordHash`를 도메인 값 객체로 변환한다.
3. 로그인 ID 중복을 검사한다.
4. 닉네임 중복을 검사한다.
5. `memberRepository.nextId()`로 새 `MemberId`를 생성한다.
6. `Member.register(...)`로 신규 회원을 만든다.
7. `memberRepository.save(member)`를 호출한다.
8. 저장된 `MemberId`를 반환한다.

값 객체 생성 중 발생하는 도메인 검증 예외는 잡거나 변환하지 않는다.

## 6. 오류 처리

이번 단계에서는 잘못된 입력과 중복 값에 대해 `IllegalArgumentException`을 던진다.

애플리케이션 서비스가 직접 던지는 메시지:

- command null: `Register member command must not be null.`
- 로그인 ID 중복: `Login id already exists.`
- 닉네임 중복: `Nickname already exists.`

값 객체 검증 실패 메시지는 기존 도메인 객체의 메시지를 그대로 사용한다.

커스텀 application exception, 도메인 exception, API error response는 이번 단계에서 도입하지 않는다. HTTP 응답과 에러 코드 설계는 API slice에서 다룬다.

## 7. 테스트 계획

모든 테스트 메서드명은 한글 시나리오형으로 작성한다. 단어 사이는 `_`로 연결한다.

테스트는 `RegisterMemberServiceTest`에서 `FakeMemberRepository`를 사용한다. Spring Context를 띄우지 않는다.

필수 테스트:

- `유효한_요청이면_회원을_저장하고_ID를_반환한다`
- `저장된_회원은_요청_값을_도메인_값으로_가진다`
- `신규_회원은_MEMBER_권한과_ACTIVE_상태를_가진다`
- `command가_null이면_저장하지_않는다`
- `로그인_ID가_이미_존재하면_저장하지_않는다`
- `닉네임이_이미_존재하면_저장하지_않는다`
- `잘못된_로그인_ID이면_저장하지_않는다`
- `잘못된_닉네임이면_저장하지_않는다`
- `잘못된_비밀번호_해시이면_저장하지_않는다`

`FakeMemberRepository`는 다음 기능을 제공한다.

- 내부 sequence로 `nextId()`를 구현한다.
- 저장된 `Member` 목록을 확인할 수 있게 한다.
- 중복 테스트를 위해 기존 로그인 ID를 등록할 수 있게 한다.
- 중복 테스트를 위해 기존 닉네임을 등록할 수 있게 한다.

중복 검사 실패와 값 객체 검증 실패 상황에서는 저장된 회원 목록이 비어 있음을 검증한다.

## 8. 검증 기준

구현 완료 기준:

- 회원가입 애플리케이션 클래스와 테스트가 `member.application` 패키지에 추가된다.
- `RegisterMemberService`는 Spring annotation 없이 순수 Java class로 남는다.
- 회원가입 애플리케이션 테스트는 Spring Context 없이 실행된다.
- `RegisterMemberCommand`는 `memberId`를 받지 않는다.
- 회원 ID는 `MemberRepository.nextId()`를 통해 생성된다.
- 로그인 ID와 닉네임 중복은 저장 전에 검사된다.
- BCrypt, JPA, REST API, JWT 관련 코드는 추가하지 않는다.
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
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional'
```

첫 번째 명령은 한글 테스트명 규칙을 확인한다. 두 번째 명령은 회원 도메인과 애플리케이션 계층에 Spring/JPA annotation이 들어가지 않았는지 확인한다.

## 9. Git

설계 문서 커밋:

- `docs: add register member application design`

구현 단계는 별도 계획 문서 승인 후 작은 단위로 진행한다. 구현 전에는 격리된 worktree를 사용한다.
