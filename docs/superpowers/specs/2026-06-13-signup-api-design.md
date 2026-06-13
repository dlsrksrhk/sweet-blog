# 회원가입 API 1차 설계

## 1. 배경

DDD Blog는 DDD와 TDD를 학습하기 위한 블로그 프로젝트다. 현재 백엔드에는 회원 도메인, 회원가입 애플리케이션 서비스, 회원 JPA 저장소 adapter, DB 기반 회원 ID 생성기가 구현되어 있다.

현재 `RegisterMemberService`는 Spring annotation이 없는 순수 Java class이며, 이미 해시 처리된 `passwordHash`를 입력으로 받는다. 다음 단계는 이 순수 회원가입 유스케이스를 HTTP API로 연결하고, API 요청의 평문 비밀번호를 BCrypt 해시로 변환하는 얇은 Spring 계층을 추가하는 것이다.

이번 단계에서는 로그인, JWT, refresh token, 내 정보 조회는 구현하지 않는다. 회원가입 요청 하나가 실제 MySQL Testcontainers 기반 persistence adapter까지 연결되는 수직 슬라이스를 만든다.

## 2. 목표

이번 작업의 목표는 `POST /api/auth/signup` 회원가입 API를 추가하는 것이다.

핵심 목표:

- 회원가입 HTTP request/response DTO를 추가한다.
- `SignupController`를 추가한다.
- Spring bean인 `SignupService`를 추가한다.
- `RawPassword` 값 객체로 평문 비밀번호 최소 길이를 검증한다.
- `SignupService`가 평문 비밀번호를 BCrypt로 해시 처리한다.
- `SignupService`가 기존 `RegisterMemberService`를 호출한다.
- 기존 `RegisterMemberService`는 순수 Java class로 유지한다.
- `RegisterMemberService`를 Spring bean으로 등록하는 configuration을 추가한다.
- `PasswordEncoder` bean을 등록한다.
- 최소 공통 예외 응답을 추가한다.
- 회원가입 성공 시 `201 Created`와 생성된 `memberId`를 반환한다.

## 3. 구현 범위

이번 작업에 포함하는 내용:

- `member.api` 패키지 추가
- `SignupController` 추가
- `SignupRequest` 추가
- `SignupResponse` 추가
- `SignupService` 추가
- `MemberApplicationConfig` 추가
- `PasswordConfig` 추가
- `SecurityConfig` 최소 설정 추가
- `GlobalExceptionHandler` 추가
- `ErrorResponse` 추가
- 평문 비밀번호 길이 검증 값 객체 추가
- 회원가입 API controller 테스트 추가
- 회원가입 API 파사드 서비스 테스트 추가

이번 작업에서 제외하는 내용:

- 로그인 API
- JWT 발급과 검증
- refresh token
- JWT 기반 Spring Security filter chain 세부 설정
- 인증된 사용자 조회
- 내 정보 조회 API
- 회원 조회 repository method
- 회원 수정/탈퇴
- 커스텀 예외 계층과 에러 코드 체계
- field error 목록 응답
- 요청 JSON 파싱 실패 응답 공통화
- Bean Validation 기반 요청 검증과 field error 응답
- OpenAPI 문서화
- 프론트엔드 회원가입 화면

## 4. 아키텍처

이번 API 슬라이스는 얇은 API 파사드 방식으로 구현한다.

```text
HTTP POST /api/auth/signup
  -> SignupController
  -> SignupService
  -> RawPassword
  -> PasswordEncoder.encode(rawPassword)
  -> RegisterMemberService.register(RegisterMemberCommand)
  -> MemberRepository / MemberIdGenerator persistence adapter
```

### 4.1 SignupController

패키지:

```text
com.dddblog.backend.member.api
```

역할:

- `POST /api/auth/signup` 요청을 받는다.
- 요청 본문을 `SignupRequest`로 받는다.
- `SignupService.signup(request)`에 위임한다.
- 생성된 회원 ID를 `SignupResponse`로 감싸 `201 Created`로 반환한다.

Controller는 비밀번호 해싱, 중복 검사, 도메인 객체 생성 규칙을 직접 처리하지 않는다.

### 4.2 SignupService

패키지:

```text
com.dddblog.backend.member.api
```

역할:

- API 요청의 평문 비밀번호를 `PasswordEncoder`로 해시 처리한다.
- 해시된 비밀번호로 `RegisterMemberCommand`를 만든다.
- 기존 `RegisterMemberService.register(command)`를 호출한다.
- 반환된 `MemberId`를 API 응답에 필요한 primitive 값으로 변환한다.

`SignupService`는 Spring bean이다. 이 계층은 Spring/Security 의존성과 순수 application service 사이의 조립 계층이다.

### 4.3 RegisterMemberService

기존 `RegisterMemberService`는 변경을 최소화한다.

- `@Service`를 붙이지 않는다.
- `PasswordEncoder`를 직접 주입하지 않는다.
- 계속 `passwordHash`를 받는다.
- 기존 도메인 값 객체 변환, 중복 검사, ID 발급, 저장 흐름을 유지한다.

이 경계를 유지하면 `member.application` 테스트는 Spring Context 없이 빠르게 실행될 수 있다.

### 4.4 Configuration

`MemberApplicationConfig`는 기존 순수 application service를 Spring bean으로 등록한다.

예상 책임:

- `MemberRepository`
- `MemberIdGenerator`
- `RegisterMemberService`

`PasswordConfig`는 `PasswordEncoder` bean을 등록한다.

예상 구현:

```text
PasswordEncoder -> BCryptPasswordEncoder
```

`SecurityConfig`는 회원가입 API가 인증 없이 호출될 수 있도록 최소 filter chain을 등록한다.

필수 정책:

- `POST /api/auth/signup`은 인증 없이 허용한다.
- 나머지 요청은 기본적으로 인증을 요구한다.
- JWT 인증은 아직 추가하지 않는다.
- REST API 테스트와 로컬 호출을 위해 CSRF는 비활성화하거나 회원가입 API에 적용되지 않게 한다.

이번 범위에서는 로그인, JWT token filter, 세션 정책 고도화, CORS 설정은 다루지 않는다.

### 4.5 RawPassword

평문 비밀번호 길이 규칙은 해시 처리 전에 검증해야 한다. 이를 위해 Spring annotation이 없는 값 객체를 추가한다.

패키지:

```text
com.dddblog.backend.member.domain
```

역할:

- 평문 비밀번호가 `null` 또는 blank이면 거부한다.
- 평문 비밀번호가 8자 미만이면 거부한다.
- `SignupService`가 `RawPassword`를 만든 뒤 `PasswordEncoder`에 원문 값을 전달한다.
- 저장하거나 로그로 노출하지 않는다.

예상 메시지:

- `Password must not be blank.`
- `Password must be at least 8 characters.`

## 5. API 설계

### 5.1 요청

Endpoint:

```http
POST /api/auth/signup
Content-Type: application/json
```

Request body:

```json
{
  "name": "홍길동",
  "nickname": "gildong",
  "loginId": "gildong123",
  "password": "password123"
}
```

`password`는 평문 비밀번호다. 이 값은 `RawPassword`로 검증한 뒤 BCrypt 해시로 변환한다. 기존 `RegisterMemberService`에는 해시 문자열만 전달한다.

### 5.2 성공 응답

회원가입 성공 시:

```http
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "memberId": 1
}
```

이번 범위에서는 `Location` header를 필수로 두지 않는다. 회원 조회 API가 아직 없기 때문이다.

### 5.3 실패 응답

유스케이스 실패는 최소 공통 오류 응답으로 반환한다.

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json
```

```json
{
  "message": "Login id already exists."
}
```

이번 단계에서 공통화하는 예외:

- `IllegalArgumentException`

`IllegalArgumentException`은 `GlobalExceptionHandler`에서 `400 Bad Request`로 변환한다.

## 6. 오류 처리

최소 공통 응답 객체:

```java
public record ErrorResponse(String message) {
}
```

기존 도메인/애플리케이션 예외 메시지는 그대로 API 응답에 노출한다.

예상 메시지 예:

- `Register member command must not be null.`
- `Login id already exists.`
- `Nickname already exists.`
- `Password must not be blank.`
- `Password must be at least 8 characters.`
- 기존 값 객체 검증 실패 메시지

이번 단계에서는 다음 오류를 별도로 공통화하지 않는다.

- JSON 문법 오류
- 알 수 없는 필드
- 누락 필드에 대한 Spring MVC binding 오류
- field-level validation 응답
- DB unique violation의 사용자 친화 메시지 변환

누락 필드는 기존 값 객체나 application service 검증을 통해 `IllegalArgumentException`으로 드러나는 범위까지만 다룬다.

## 7. 테스트 계획

모든 백엔드 테스트 메서드명은 한글 시나리오형으로 작성한다. 단어 사이는 `_`로 연결한다.

### 7.1 SignupServiceTest

`SignupServiceTest`는 Spring Context 없이 실행한다.

검증 목적:

- 평문 비밀번호가 해시되어 `RegisterMemberService`에 전달된다.
- 짧은 평문 비밀번호는 해시하거나 저장하지 않고 거부된다.
- API 요청 값이 `RegisterMemberCommand`로 변환된다.
- 기존 `RegisterMemberService`가 반환한 `MemberId`를 primitive 응답 값으로 변환한다.

필수 시나리오:

- `회원가입을_요청하면_비밀번호를_해시해서_회원가입_서비스에_전달한다`
- `회원가입에_성공하면_회원_ID를_반환한다`
- `비밀번호가_8자_미만이면_회원가입_서비스를_호출하지_않는다`

테스트 대역은 필요한 만큼만 둔다. `PasswordEncoder`는 고정 문자열을 반환하는 fake로 둔다. `RegisterMemberService`는 실제 객체를 사용하고, 그 의존성인 `MemberRepository`와 `MemberIdGenerator`를 fake로 두어 저장된 회원의 `PasswordHash`를 확인한다.

### 7.2 SignupControllerTest

`SignupControllerTest`는 MVC slice로 작성한다.

필수 시나리오:

- `회원가입에_성공하면_201과_회원_ID를_반환한다`
- `회원가입_요청이_실패하면_400과_오류_메시지를_반환한다`
- `인증_없이_회원가입을_요청할_수_있다`

Controller 테스트는 실제 JPA나 MySQL을 사용하지 않는다. `SignupService`는 mock 또는 test double로 대체한다.

### 7.3 RawPasswordTest

`RawPasswordTest`는 Spring Context 없이 실행한다.

필수 시나리오:

- `비밀번호가_null이면_생성할_수_없다`
- `비밀번호가_blank이면_생성할_수_없다`
- `비밀번호가_8자_미만이면_생성할_수_없다`
- `유효한_비밀번호이면_원문_값을_반환한다`

### 7.4 Wiring 테스트

필요하면 전체 Spring Context 수준에서 다음 bean 조립만 얇게 확인한다.

- `RegisterMemberService`
- `SignupService`
- `PasswordEncoder`
- `SecurityFilterChain`

단, 구현 중 기존 테스트만으로 충분하면 별도 wiring 테스트는 추가하지 않는다.

## 8. 보안과 비밀번호 처리

회원가입 API는 평문 비밀번호를 요청으로 받는다. 평문 비밀번호는 다음 경계 밖으로 나가지 않는다.

```text
SignupRequest.password -> SignupService -> RawPassword -> PasswordEncoder.encode(...)
```

`RegisterMemberCommand`에는 해시 문자열만 들어간다. `Member`와 `PasswordHash`는 해시 문자열만 보관한다.

이번 단계에서는 평문 비밀번호의 최소 길이만 검증한다. 대문자, 숫자, 특수문자 조합 같은 복잡도 정책은 초기 범위에서 제외한다.

## 9. 검증 기준

구현 완료 기준:

- `POST /api/auth/signup` API가 추가된다.
- 성공 시 `201 Created`와 `{ "memberId": ... }`를 반환한다.
- API 요청의 `password`는 BCrypt 해시로 변환된 뒤 기존 회원가입 서비스에 전달된다.
- API 요청의 `password`는 8자 이상이어야 한다.
- `RegisterMemberService`는 Spring annotation 없이 순수 Java class로 남는다.
- `member.domain`과 `member.application`에는 Spring/JPA annotation이 추가되지 않는다.
- `POST /api/auth/signup`은 인증 없이 호출할 수 있다.
- `IllegalArgumentException`은 `{ "message": ... }` 형태의 `400 Bad Request`로 반환된다.
- 회원가입 API 테스트와 파사드 서비스 테스트가 추가된다.
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

## 10. Git

설계 문서 커밋:

```text
docs: add signup api design
```

구현 단계는 별도 계획 문서 승인 후 진행한다. 구현 전에는 격리된 worktree를 사용한다.
