# 로그인과 JWT 인증 1차 설계

## 1. 배경

DDD Blog는 DDD와 TDD를 학습하기 위한 블로그 프로젝트다. 현재 백엔드에는 회원 도메인, 회원가입 애플리케이션 서비스, 회원 JPA 저장소 adapter, 회원가입 API, BCrypt 비밀번호 해시 처리, 최소 공통 오류 응답이 구현되어 있다.

다음 단계는 회원가입으로 저장된 회원이 로그인하고, JWT Access Token을 받아 인증이 필요한 API를 호출할 수 있게 만드는 것이다. 이 인증 기반은 이후 내 글 작성, 수정, 삭제, 비공개 글 조회 같은 작성자 권한 검증의 전제가 된다.

이번 단계에서는 로그인, JWT 발급과 검증, 현재 로그인한 회원 정보 조회, 로그아웃 처리 방식을 다룬다. Refresh Token, 토큰 재발급, 서버 측 로그아웃 상태 관리, JWT blacklist는 구현하지 않는다.

## 2. 목표

이번 작업의 목표는 인증 수직 슬라이스를 추가하는 것이다.

핵심 목표:

- `POST /api/auth/login` 로그인 API를 추가한다.
- 로그인 성공 시 JWT Access Token을 반환한다.
- JWT에는 회원 ID와 권한을 포함한다.
- `Authorization: Bearer <token>` 요청을 검증하는 Spring Security 필터를 추가한다.
- 인증된 사용자 정보를 Spring Security 인증 객체로 제공한다.
- `GET /api/members/me` 내 정보 조회 API를 추가한다.
- 인증 실패는 `401 Unauthorized`와 `{ "message": "Authentication failed." }`로 통일한다.
- 로그아웃은 클라이언트가 Access Token을 삭제하는 방식으로 문서화한다.

## 3. 구현 범위

이번 작업에 포함하는 내용:

- `auth.api` 패키지 추가
- `auth.application` 패키지 추가
- `auth.security` 패키지 추가
- 로그인 요청/응답 DTO 추가
- 로그인 유스케이스 서비스 추가
- JWT 발급/검증 컴포넌트 추가
- JWT 인증 필터 추가
- 인증 principal 추가
- 인증 실패 응답 처리 추가
- `GET /api/members/me` API 추가
- 회원 조회용 repository method 추가
- 로그인, JWT, 내 정보 조회 테스트 추가
- 회원가입부터 로그인, 내 정보 조회까지 이어지는 수직 통합 테스트 추가

이번 작업에서 제외하는 내용:

- Refresh Token
- Access Token 재발급
- 서버 측 로그아웃 API
- JWT blacklist
- 세션 기반 인증
- OAuth2 로그인
- 관리자 API
- 회원 정보 수정/탈퇴
- 가입일 응답
- auditing 컬럼 추가
- 글 작성 API
- 작성자 권한 검증
- CORS 세부 설정
- OpenAPI 문서화
- 프론트엔드 로그인 화면

## 4. 아키텍처

이번 인증 슬라이스는 `auth` 패키지를 별도로 두고, 회원 정보 조회는 `member` application port를 통해 접근한다.

```text
POST /api/auth/login
  -> LoginController
  -> LoginService
  -> MemberRepository.findByLoginId(LoginId)
  -> PasswordEncoder.matches(rawPassword, passwordHash)
  -> JwtTokenProvider.createAccessToken(memberId, role)
  -> LoginResponse
```

```text
GET /api/members/me
  -> JwtAuthenticationFilter
  -> JwtTokenProvider.parseAccessToken(token)
  -> SecurityContext(AuthenticatedMember)
  -> MeController
  -> MemberRepository.findById(MemberId)
  -> MeResponse
```

### 4.1 Auth API

패키지:

```text
com.dddblog.backend.auth.api
```

구성:

- `LoginController`
- `LoginRequest`
- `LoginResponse`

`LoginController`는 `POST /api/auth/login` 요청을 받고 `LoginService`에 위임한다. 성공하면 `200 OK`와 Access Token을 반환한다.

### 4.2 Auth Application

패키지:

```text
com.dddblog.backend.auth.application
```

구성:

- `LoginService`

`LoginService`는 로그인 유스케이스를 표현한다. 저장된 회원을 login ID로 조회하고, BCrypt password encoder로 평문 비밀번호와 저장된 해시를 비교한다. 회원이 없거나 비밀번호가 맞지 않거나 회원 상태가 `ACTIVE`가 아니면 같은 인증 실패 예외를 던진다.

### 4.3 Auth Security

패키지:

```text
com.dddblog.backend.auth.security
```

구성:

- `JwtTokenProvider`
- `JwtAuthenticationFilter`
- `AuthenticatedMember`
- `JwtAuthenticationEntryPoint`
- JWT 설정 properties

`JwtTokenProvider`는 JWT Access Token 생성과 검증을 담당한다. JWT 구현은 `jjwt` 라이브러리를 사용한다.

`JwtAuthenticationFilter`는 `Authorization` 헤더의 Bearer Token을 검증한다. 유효한 토큰이면 `SecurityContext`에 인증 객체를 저장하고, 잘못된 토큰이면 `401 Unauthorized`를 반환한다. Bearer Token이 없으면 다음 필터로 진행하고, 보호 API 접근 여부는 Spring Security authorization 설정이 판단한다.

### 4.4 Member 조회 포트

기존 `MemberRepository`에 조회 method를 최소 추가한다.

```java
Optional<Member> findByLoginId(LoginId loginId);
Optional<Member> findById(MemberId memberId);
```

회원가입 흐름에서 사용하던 중복 검사와 저장 method는 유지한다. `member.domain`과 `member.application`에는 Spring/JPA annotation을 추가하지 않는다.

## 5. API 설계

### 5.1 로그인 요청

Endpoint:

```http
POST /api/auth/login
Content-Type: application/json
```

Request body:

```json
{
  "loginId": "gildong123",
  "password": "password123"
}
```

### 5.2 로그인 성공 응답

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "accessToken": "eyJ..."
}
```

이번 범위에서는 token type, expiresIn, refreshToken은 응답에 포함하지 않는다.

### 5.3 내 정보 조회 요청

Endpoint:

```http
GET /api/members/me
Authorization: Bearer <accessToken>
```

### 5.4 내 정보 조회 성공 응답

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "memberId": 1,
  "name": "홍길동",
  "nickname": "gildong",
  "loginId": "gildong123",
  "role": "MEMBER"
}
```

요구사항에는 가입일이 있지만 현재 `members` table과 JPA entity에 auditing column이 없다. 이번 인증 마일스톤에서는 auditing column을 도입하지 않으므로 `createdAt`은 응답에 포함하지 않는다.

### 5.5 로그아웃

초기 로그아웃은 서버 API를 만들지 않는다. 클라이언트가 저장한 Access Token을 삭제하면 로그아웃된 것으로 본다.

Refresh Token을 도입하는 이후 단계에서는 서버 측 토큰 무효화 정책을 별도 설계한다.

## 6. JWT 설계

JWT claims는 최소로 유지한다.

```json
{
  "sub": "1",
  "role": "MEMBER",
  "type": "access",
  "iat": 1718611200,
  "exp": 1718614800
}
```

Claim 의미:

- `sub`: 회원 ID
- `role`: 회원 권한
- `type`: access token 구분
- `iat`: 발급 시각
- `exp`: 만료 시각

닉네임, 이름, 로그인 ID는 토큰에 넣지 않는다. 변경 가능성이 있는 프로필 정보는 `/api/members/me`에서 DB를 조회해 반환한다.

설정값:

```properties
app.jwt.secret=local-development-secret
app.jwt.access-token-validity-seconds=3600
```

이번 프로젝트는 학습용 로컬 실행 중심이므로 설정 파일 기반으로 시작한다. 운영 수준의 secret 관리와 key rotation은 제외한다.

## 7. 보안 정책

공개 endpoint:

```text
POST /api/auth/signup
POST /api/auth/login
```

보호 endpoint:

```text
GET /api/members/me
그 외 기본적으로 authenticated
```

Spring Security 설정:

- CSRF는 비활성화한다.
- SessionCreationPolicy는 `STATELESS`로 둔다.
- `httpBasic`은 활성화하지 않는다.
- `formLogin`은 활성화하지 않는다.
- JWT 인증 필터를 username/password 인증 필터 앞에 배치한다.

## 8. 오류 처리

인증 실패는 응답 shape와 메시지를 통일한다.

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json
```

```json
{
  "message": "Authentication failed."
}
```

다음 경우 모두 같은 응답을 사용한다.

- 로그인 ID가 존재하지 않는다.
- 비밀번호가 일치하지 않는다.
- 회원 상태가 `ACTIVE`가 아니다.
- Bearer Token이 위조되었다.
- Bearer Token이 만료되었다.
- Bearer Token이 access token이 아니다.
- 토큰의 subject가 더 이상 존재하는 회원을 가리키지 않는다.

토큰 없이 보호 API를 호출한 경우도 `401 Unauthorized`와 같은 응답 shape를 사용한다.

기존 `IllegalArgumentException -> 400 Bad Request` 응답은 유지한다.

## 9. 테스트 계획

모든 백엔드 테스트 메서드명은 한글 시나리오형으로 작성하고 단어 사이는 `_`로 연결한다.

### 9.1 LoginServiceTest

Spring Context 없이 실행한다.

필수 시나리오:

- `올바른_로그인_ID와_비밀번호이면_액세스_토큰을_발급한다`
- `존재하지_않는_로그인_ID이면_인증에_실패한다`
- `비밀번호가_일치하지_않으면_인증에_실패한다`
- `비활성_회원이면_인증에_실패한다`

### 9.2 JwtTokenProviderTest

`Clock`을 주입해 시간 테스트를 안정화한다.

필수 시나리오:

- `액세스_토큰을_생성하면_회원_ID와_권한을_파싱할_수_있다`
- `만료된_토큰이면_검증에_실패한다`
- `위조된_토큰이면_검증에_실패한다`
- `액세스_토큰이_아니면_검증에_실패한다`

### 9.3 LoginControllerTest

MVC slice로 실행한다.

필수 시나리오:

- `로그인에_성공하면_200과_액세스_토큰을_반환한다`
- `로그인에_실패하면_401과_오류_메시지를_반환한다`

### 9.4 MeControllerTest

MVC slice에 Spring Security 설정을 포함해 실행한다.

필수 시나리오:

- `인증된_요청이면_내_정보를_반환한다`
- `토큰이_없으면_401을_반환한다`
- `잘못된_토큰이면_401을_반환한다`

### 9.5 LoginApiIntegrationTest

MySQL Testcontainers 기반 수직 통합 테스트로 실행한다.

필수 시나리오:

- `회원가입_후_로그인하면_발급된_토큰으로_내_정보를_조회할_수_있다`

검증 내용:

- 회원가입 API로 회원을 생성한다.
- 로그인 API로 Access Token을 발급받는다.
- 발급받은 Access Token으로 `/api/members/me`를 호출한다.
- 응답의 회원 ID, 이름, 닉네임, 로그인 ID, 권한이 가입한 회원과 일치한다.
- 응답에 비밀번호 해시는 포함되지 않는다.

## 10. 검증 기준

구현 완료 기준:

- `POST /api/auth/login` API가 추가된다.
- 올바른 로그인 ID와 비밀번호면 JWT Access Token을 반환한다.
- 로그인 실패는 `401 Unauthorized`와 `{ "message": "Authentication failed." }`를 반환한다.
- `Authorization: Bearer <token>` 인증이 동작한다.
- 유효하지 않은 토큰은 `401 Unauthorized`를 반환한다.
- 토큰 없이 보호 API를 호출하면 `401 Unauthorized`를 반환한다.
- `GET /api/members/me`가 인증된 회원 정보를 반환한다.
- 내 정보 응답에 비밀번호 해시는 포함되지 않는다.
- 로그아웃은 클라이언트 토큰 삭제 방식으로 문서화된다.
- Refresh Token, token blacklist, 서버 로그아웃 API는 구현하지 않는다.
- `member.domain`과 `member.application`에는 Spring/JPA annotation이 추가되지 않는다.
- 전체 백엔드 테스트가 통과한다.
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
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional|@Configuration|@Bean'
```

위 두 규칙 확인 명령은 출력이 없어야 한다.

## 11. Git

설계 문서 커밋:

```text
docs: add login jwt auth design
```

구현 단계는 별도 계획 문서 승인 후 진행한다.
