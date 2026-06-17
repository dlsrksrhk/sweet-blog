# Login JWT Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add login, JWT Access Token issuance/verification, authenticated member lookup, and client-side logout documentation.

**Architecture:** Authentication lives in a new `auth` package. The auth layer issues and verifies JWTs, while member data is read through the existing pure `MemberRepository` port. `/api/members/me` uses the authenticated principal's member ID and never parses tokens directly in the controller.

**Tech Stack:** Java 21, Spring Boot 3.5.0, Spring Security, Spring MVC, Spring Data JPA, MySQL Testcontainers, JJWT 0.13.0, JUnit 5, AssertJ, MockMvc.

---

## Scope And References

Spec:

- `docs/superpowers/specs/2026-06-17-login-jwt-auth-design.md`

Existing patterns to follow:

- `backend/src/main/java/com/dddblog/backend/member/api/SignupController.java`
- `backend/src/main/java/com/dddblog/backend/member/api/SignupService.java`
- `backend/src/main/java/com/dddblog/backend/common/api/ErrorResponse.java`
- `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`
- `backend/src/test/java/com/dddblog/backend/member/api/SignupControllerTest.java`
- `backend/src/test/java/com/dddblog/backend/member/api/SignupApiIntegrationTest.java`

Local rules:

- Backend test method names must be Korean scenario-style names with `_`.
- Pure domain/application tests must not start Spring Context.
- `member.domain` and `member.application` must remain free of Spring/JPA annotations.
- Use TDD: write a small failing test, verify failure, implement minimum code, verify pass, commit.

---

## File Structure

### New auth files

- `backend/src/main/java/com/dddblog/backend/auth/api/LoginController.java`
  - Handles `POST /api/auth/login`.
- `backend/src/main/java/com/dddblog/backend/auth/api/LoginRequest.java`
  - Request DTO with `loginId` and `password`.
- `backend/src/main/java/com/dddblog/backend/auth/api/LoginResponse.java`
  - Response DTO with `accessToken`.
- `backend/src/main/java/com/dddblog/backend/auth/application/LoginService.java`
  - Pure login use case orchestration except for the injected `PasswordEncoder` and token issuer port.
- `backend/src/main/java/com/dddblog/backend/auth/application/AccessTokenIssuer.java`
  - Small port so `LoginServiceTest` can run without JJWT.
- `backend/src/main/java/com/dddblog/backend/auth/application/AuthenticationFailedException.java`
  - Runtime exception for all login/authentication failures.
- `backend/src/main/java/com/dddblog/backend/auth/security/AuthenticatedMember.java`
  - Principal data: `MemberId memberId`, `MemberRole role`.
- `backend/src/main/java/com/dddblog/backend/auth/security/JwtAuthentication.java`
  - Spring `Authentication` implementation backed by `AuthenticatedMember`.
- `backend/src/main/java/com/dddblog/backend/auth/security/JwtAuthenticationEntryPoint.java`
  - Writes `401 {"message":"Authentication failed."}` for missing/invalid auth.
- `backend/src/main/java/com/dddblog/backend/auth/security/JwtAuthenticationFilter.java`
  - Reads Bearer Token, validates it, sets `SecurityContext`.
- `backend/src/main/java/com/dddblog/backend/auth/security/JwtProperties.java`
  - Binds `app.jwt.secret` and `app.jwt.access-token-validity-seconds`.
- `backend/src/main/java/com/dddblog/backend/auth/security/JwtTokenProvider.java`
  - Creates and parses Access Tokens with JJWT.
- `backend/src/main/java/com/dddblog/backend/auth/security/ParsedAccessToken.java`
  - Parsed token data: `MemberId memberId`, `MemberRole role`.

### New member API files

- `backend/src/main/java/com/dddblog/backend/member/api/MeController.java`
  - Handles `GET /api/members/me`.
- `backend/src/main/java/com/dddblog/backend/member/api/MeResponse.java`
  - Response DTO for member ID, name, nickname, login ID, role.
- `backend/src/main/java/com/dddblog/backend/member/api/MeService.java`
  - Reads the current member through `MemberRepository`.

### Modified production files

- `backend/build.gradle.kts`
  - Add JJWT dependencies.
- `backend/src/main/resources/application.properties`
  - Add local JWT properties.
- `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`
  - Permit login, configure stateless sessions, register JWT filter and entry point.
- `backend/src/main/java/com/dddblog/backend/member/application/MemberRepository.java`
  - Add `findByLoginId` and `findById`.
- `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapter.java`
  - Implement read methods.
- `backend/src/main/java/com/dddblog/backend/member/persistence/SpringDataJpaMemberRepository.java`
  - Add Spring Data read methods.
- `docs/handoff.md`
  - Refresh completed milestone notes and next-step guidance.

### New and modified test files

- `backend/src/test/java/com/dddblog/backend/auth/application/LoginServiceTest.java`
- `backend/src/test/java/com/dddblog/backend/auth/security/JwtTokenProviderTest.java`
- `backend/src/test/java/com/dddblog/backend/auth/api/LoginControllerTest.java`
- `backend/src/test/java/com/dddblog/backend/member/api/MeControllerTest.java`
- `backend/src/test/java/com/dddblog/backend/auth/api/LoginApiIntegrationTest.java`
- `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberRepository.java`
- `backend/src/test/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapterTest.java`

---

## Task 1: Add JJWT Dependencies And JWT Properties

**Files:**

- Modify: `backend/build.gradle.kts`
- Modify: `backend/src/main/resources/application.properties`

- [ ] **Step 1: Add JJWT dependencies**

Modify `backend/build.gradle.kts` dependencies block:

```kotlin
dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("io.jsonwebtoken:jjwt-api:0.13.0")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.mysql:mysql-connector-j")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mysql")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}
```

- [ ] **Step 2: Add local JWT properties**

Append to `backend/src/main/resources/application.properties`:

```properties
app.jwt.secret=local-development-secret-key-that-is-long-enough-for-hmac-sha256
app.jwt.access-token-validity-seconds=3600
```

- [ ] **Step 3: Verify dependency resolution**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat compileJava
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 4: Commit**

```powershell
git add backend/build.gradle.kts backend/src/main/resources/application.properties
git commit -m "build: add jwt dependencies"
```

---

## Task 2: Add JWT Token Provider

**Files:**

- Create: `backend/src/main/java/com/dddblog/backend/auth/application/AccessTokenIssuer.java`
- Create: `backend/src/main/java/com/dddblog/backend/auth/security/JwtProperties.java`
- Create: `backend/src/main/java/com/dddblog/backend/auth/security/ParsedAccessToken.java`
- Create: `backend/src/main/java/com/dddblog/backend/auth/security/JwtTokenProvider.java`
- Test: `backend/src/test/java/com/dddblog/backend/auth/security/JwtTokenProviderTest.java`

- [ ] **Step 1: Write failing JWT tests**

Create `backend/src/test/java/com/dddblog/backend/auth/security/JwtTokenProviderTest.java`:

```java
package com.dddblog.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

class JwtTokenProviderTest {

	private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha256";
	private static final Instant NOW = Instant.parse("2026-06-17T00:00:00Z");

	@Test
	void 액세스_토큰을_생성하면_회원_ID와_권한을_파싱할_수_있다() {
		JwtTokenProvider tokenProvider = tokenProviderAt(NOW, 3600);

		String token = tokenProvider.createAccessToken(new MemberId(1L), MemberRole.MEMBER);

		ParsedAccessToken parsedToken = tokenProvider.parseAccessToken(token);
		assertThat(parsedToken.memberId()).isEqualTo(new MemberId(1L));
		assertThat(parsedToken.role()).isEqualTo(MemberRole.MEMBER);
	}

	@Test
	void 만료된_토큰이면_검증에_실패한다() {
		JwtTokenProvider issuer = tokenProviderAt(NOW, 1);
		String token = issuer.createAccessToken(new MemberId(1L), MemberRole.MEMBER);
		JwtTokenProvider verifier = tokenProviderAt(NOW.plusSeconds(2), 1);

		assertThatThrownBy(() -> verifier.parseAccessToken(token))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Authentication failed.");
	}

	@Test
	void 위조된_토큰이면_검증에_실패한다() {
		JwtTokenProvider tokenProvider = tokenProviderAt(NOW, 3600);
		String token = tokenProvider.createAccessToken(new MemberId(1L), MemberRole.MEMBER);
		String tamperedToken = token.substring(0, token.length() - 2) + "xx";

		assertThatThrownBy(() -> tokenProvider.parseAccessToken(tamperedToken))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Authentication failed.");
	}

	@Test
	void 액세스_토큰이_아니면_검증에_실패한다() {
		JwtTokenProvider tokenProvider = tokenProviderAt(NOW, 3600);
		String token = tokenProvider.createToken(new MemberId(1L), MemberRole.MEMBER, "refresh");

		assertThatThrownBy(() -> tokenProvider.parseAccessToken(token))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Authentication failed.");
	}

	private JwtTokenProvider tokenProviderAt(Instant now, long validitySeconds) {
		return new JwtTokenProvider(
			new JwtProperties(SECRET, validitySeconds),
			Clock.fixed(now, ZoneOffset.UTC)
		);
	}
}
```

- [ ] **Step 2: Run JWT tests and verify they fail**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "*JwtTokenProviderTest"
```

Expected:

```text
Compilation failed
```

The missing types should include `JwtTokenProvider`, `JwtProperties`, and `ParsedAccessToken`.

- [ ] **Step 3: Create token issuer port**

Create `backend/src/main/java/com/dddblog/backend/auth/application/AccessTokenIssuer.java`:

```java
package com.dddblog.backend.auth.application;

import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

public interface AccessTokenIssuer {

	String createAccessToken(MemberId memberId, MemberRole role);
}
```

- [ ] **Step 4: Create JWT properties**

Create `backend/src/main/java/com/dddblog/backend/auth/security/JwtProperties.java`:

```java
package com.dddblog.backend.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long accessTokenValiditySeconds) {

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("JWT secret must not be blank.");
		}
		if (accessTokenValiditySeconds <= 0) {
			throw new IllegalArgumentException("JWT access token validity seconds must be positive.");
		}
	}
}
```

- [ ] **Step 5: Create parsed token record**

Create `backend/src/main/java/com/dddblog/backend/auth/security/ParsedAccessToken.java`:

```java
package com.dddblog.backend.auth.security;

import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

public record ParsedAccessToken(MemberId memberId, MemberRole role) {

	public ParsedAccessToken {
		if (memberId == null) {
			throw new IllegalArgumentException("Member id must not be null.");
		}
		if (role == null) {
			throw new IllegalArgumentException("Member role must not be null.");
		}
	}
}
```

- [ ] **Step 6: Implement JWT token provider**

Create `backend/src/main/java/com/dddblog/backend/auth/security/JwtTokenProvider.java`:

```java
package com.dddblog.backend.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.dddblog.backend.auth.application.AccessTokenIssuer;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider implements AccessTokenIssuer {

	private static final String ACCESS_TOKEN_TYPE = "access";
	private static final String TOKEN_TYPE_CLAIM = "type";
	private static final String ROLE_CLAIM = "role";

	private final JwtProperties properties;
	private final Clock clock;
	private final SecretKey key;

	public JwtTokenProvider(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public String createAccessToken(MemberId memberId, MemberRole role) {
		return createToken(memberId, role, ACCESS_TOKEN_TYPE);
	}

	String createToken(MemberId memberId, MemberRole role, String tokenType) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plusSeconds(properties.accessTokenValiditySeconds());
		return Jwts.builder()
			.subject(memberId.value().toString())
			.claim(ROLE_CLAIM, role.name())
			.claim(TOKEN_TYPE_CLAIM, tokenType)
			.issuedAt(Date.from(issuedAt))
			.expiration(Date.from(expiresAt))
			.signWith(key)
			.compact();
	}

	public ParsedAccessToken parseAccessToken(String token) {
		try {
			Claims claims = Jwts.parser()
				.verifyWith(key)
				.clock(() -> Date.from(clock.instant()))
				.build()
				.parseSignedClaims(token)
				.getPayload();
			if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
				throw new IllegalArgumentException("Authentication failed.");
			}
			return new ParsedAccessToken(
				new MemberId(Long.valueOf(claims.getSubject())),
				MemberRole.valueOf(claims.get(ROLE_CLAIM, String.class))
			);
		}
		catch (RuntimeException exception) {
			throw new IllegalArgumentException("Authentication failed.", exception);
		}
	}
}
```

- [ ] **Step 7: Add Clock and configuration properties beans**

Modify `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`:

```java
package com.dddblog.backend.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import com.dddblog.backend.auth.security.JwtProperties;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
				.anyRequest().authenticated()
			)
			.build();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
```

- [ ] **Step 8: Run JWT tests and verify they pass**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "*JwtTokenProviderTest"
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 9: Commit**

```powershell
git add backend/src/main/java/com/dddblog/backend/auth/application/AccessTokenIssuer.java backend/src/main/java/com/dddblog/backend/auth/security/JwtProperties.java backend/src/main/java/com/dddblog/backend/auth/security/ParsedAccessToken.java backend/src/main/java/com/dddblog/backend/auth/security/JwtTokenProvider.java backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java backend/src/test/java/com/dddblog/backend/auth/security/JwtTokenProviderTest.java
git commit -m "feat: add jwt token provider"
```

---

## Task 3: Add Member Repository Read Methods

**Files:**

- Modify: `backend/src/main/java/com/dddblog/backend/member/application/MemberRepository.java`
- Modify: `backend/src/main/java/com/dddblog/backend/member/persistence/SpringDataJpaMemberRepository.java`
- Modify: `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapter.java`
- Modify: `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberRepository.java`
- Test: `backend/src/test/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapterTest.java`

- [ ] **Step 1: Add failing persistence tests**

Append these tests to `JpaMemberRepositoryAdapterTest`:

```java
	@Test
	void 로그인_ID로_회원을_조회할_수_있다() {
		memberRepository.save(createMember(40L, "user40", "닉네임40"));
		entityManager.flush();
		entityManager.clear();

		Member foundMember = memberRepository.findByLoginId(new LoginId("user40")).orElseThrow();

		assertThat(foundMember.id()).isEqualTo(new MemberId(40L));
		assertThat(foundMember.name()).isEqualTo(new MemberName("홍길동"));
		assertThat(foundMember.nickname()).isEqualTo(new Nickname("닉네임40"));
		assertThat(foundMember.loginId()).isEqualTo(new LoginId("user40"));
		assertThat(foundMember.passwordHash()).isEqualTo(new PasswordHash("hashed-password"));
		assertThat(foundMember.role()).isEqualTo(MemberRole.MEMBER);
		assertThat(foundMember.status()).isEqualTo(MemberStatus.ACTIVE);
	}

	@Test
	void 회원_ID로_회원을_조회할_수_있다() {
		memberRepository.save(createMember(50L, "user50", "닉네임50"));
		entityManager.flush();
		entityManager.clear();

		Member foundMember = memberRepository.findById(new MemberId(50L)).orElseThrow();

		assertThat(foundMember.id()).isEqualTo(new MemberId(50L));
		assertThat(foundMember.loginId()).isEqualTo(new LoginId("user50"));
	}

	@Test
	void 존재하지_않는_로그인_ID이면_빈_결과를_반환한다() {
		assertThat(memberRepository.findByLoginId(new LoginId("missing"))).isEmpty();
	}

	@Test
	void 존재하지_않는_회원_ID이면_빈_결과를_반환한다() {
		assertThat(memberRepository.findById(new MemberId(999L))).isEmpty();
	}
```

- [ ] **Step 2: Run persistence tests and verify they fail**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "*JpaMemberRepositoryAdapterTest"
```

Expected:

```text
Compilation failed
```

The missing methods should be `findByLoginId` and `findById` on `MemberRepository`.

- [ ] **Step 3: Add repository port methods**

Modify `backend/src/main/java/com/dddblog/backend/member/application/MemberRepository.java`:

```java
package com.dddblog.backend.member.application;

import java.util.Optional;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;

public interface MemberRepository {

	boolean existsByLoginId(LoginId loginId);

	boolean existsByNickname(Nickname nickname);

	Optional<Member> findByLoginId(LoginId loginId);

	Optional<Member> findById(MemberId memberId);

	MemberId save(Member member);
}
```

- [ ] **Step 4: Add Spring Data read method**

Modify `backend/src/main/java/com/dddblog/backend/member/persistence/SpringDataJpaMemberRepository.java`:

```java
package com.dddblog.backend.member.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJpaMemberRepository extends JpaRepository<JpaMemberEntity, Long> {

	boolean existsByLoginId(String loginId);

	boolean existsByNickname(String nickname);

	Optional<JpaMemberEntity> findByLoginId(String loginId);
}
```

- [ ] **Step 5: Implement adapter read methods and mapper**

Modify `backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapter.java`:

```java
package com.dddblog.backend.member.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

import jakarta.persistence.EntityManager;

@Repository
public class JpaMemberRepositoryAdapter implements MemberRepository {

	private final SpringDataJpaMemberRepository memberRepository;
	private final EntityManager entityManager;

	public JpaMemberRepositoryAdapter(SpringDataJpaMemberRepository memberRepository, EntityManager entityManager) {
		this.memberRepository = memberRepository;
		this.entityManager = entityManager;
	}

	@Override
	public boolean existsByLoginId(LoginId loginId) {
		return memberRepository.existsByLoginId(loginId.value());
	}

	@Override
	public boolean existsByNickname(Nickname nickname) {
		return memberRepository.existsByNickname(nickname.value());
	}

	@Override
	public Optional<Member> findByLoginId(LoginId loginId) {
		return memberRepository.findByLoginId(loginId.value())
			.map(this::toDomain);
	}

	@Override
	public Optional<Member> findById(MemberId memberId) {
		return memberRepository.findById(memberId.value())
			.map(this::toDomain);
	}

	@Override
	@Transactional
	public MemberId save(Member member) {
		if (member == null) {
			throw new IllegalArgumentException("Member must not be null.");
		}
		JpaMemberEntity entity = new JpaMemberEntity(
			member.id().value(),
			member.name().value(),
			member.nickname().value(),
			member.loginId().value(),
			member.passwordHash().value(),
			member.role(),
			member.status()
		);
		entityManager.persist(entity);
		return new MemberId(entity.id());
	}

	private Member toDomain(JpaMemberEntity entity) {
		return new Member(
			new MemberId(entity.id()),
			new MemberName(entity.name()),
			new Nickname(entity.nickname()),
			new LoginId(entity.loginId()),
			new PasswordHash(entity.passwordHash()),
			entity.role(),
			entity.status()
		);
	}
}
```

- [ ] **Step 6: Update fake repository**

Modify `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberRepository.java`:

```java
package com.dddblog.backend.member.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;

class FakeMemberRepository implements MemberRepository {

	private final List<Member> savedMembers = new ArrayList<>();
	private final Set<LoginId> existingLoginIds = new HashSet<>();
	private final Set<Nickname> existingNicknames = new HashSet<>();

	@Override
	public boolean existsByLoginId(LoginId loginId) {
		return existingLoginIds.contains(loginId);
	}

	@Override
	public boolean existsByNickname(Nickname nickname) {
		return existingNicknames.contains(nickname);
	}

	@Override
	public Optional<Member> findByLoginId(LoginId loginId) {
		return savedMembers.stream()
			.filter(member -> member.loginId().equals(loginId))
			.findFirst();
	}

	@Override
	public Optional<Member> findById(MemberId memberId) {
		return savedMembers.stream()
			.filter(member -> member.id().equals(memberId))
			.findFirst();
	}

	@Override
	public MemberId save(Member member) {
		savedMembers.add(member);
		existingLoginIds.add(member.loginId());
		existingNicknames.add(member.nickname());
		return member.id();
	}

	List<Member> savedMembers() {
		return List.copyOf(savedMembers);
	}

	FakeMemberRepository addExistingLoginId(LoginId loginId) {
		existingLoginIds.add(loginId);
		return this;
	}

	FakeMemberRepository addExistingNickname(Nickname nickname) {
		existingNicknames.add(nickname);
		return this;
	}
}
```

- [ ] **Step 7: Run persistence tests and affected application tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "*JpaMemberRepositoryAdapterTest" --tests "*RegisterMemberServiceTest"
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/dddblog/backend/member/application/MemberRepository.java backend/src/main/java/com/dddblog/backend/member/persistence/SpringDataJpaMemberRepository.java backend/src/main/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapter.java backend/src/test/java/com/dddblog/backend/member/application/FakeMemberRepository.java backend/src/test/java/com/dddblog/backend/member/persistence/JpaMemberRepositoryAdapterTest.java
git commit -m "feat: add member repository read methods"
```

---

## Task 4: Add Login Service

**Files:**

- Create: `backend/src/main/java/com/dddblog/backend/auth/application/AuthenticationFailedException.java`
- Create: `backend/src/main/java/com/dddblog/backend/auth/application/LoginService.java`
- Test: `backend/src/test/java/com/dddblog/backend/auth/application/LoginServiceTest.java`

- [ ] **Step 1: Write failing login service tests**

Create `backend/src/test/java/com/dddblog/backend/auth/application/LoginServiceTest.java`:

```java
package com.dddblog.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.MemberRole;
import com.dddblog.backend.member.domain.MemberStatus;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

class LoginServiceTest {

	private final FakeMemberRepository memberRepository = new FakeMemberRepository();
	private final FakePasswordEncoder passwordEncoder = new FakePasswordEncoder();
	private final FakeAccessTokenIssuer tokenIssuer = new FakeAccessTokenIssuer();
	private final LoginService loginService = new LoginService(memberRepository, passwordEncoder, tokenIssuer);

	@Test
	void 올바른_로그인_ID와_비밀번호이면_액세스_토큰을_발급한다() {
		memberRepository.save(activeMember());

		String accessToken = loginService.login("user01", "password123");

		assertThat(accessToken).isEqualTo("token-1-MEMBER");
	}

	@Test
	void 존재하지_않는_로그인_ID이면_인증에_실패한다() {
		assertThatThrownBy(() -> loginService.login("missing", "password123"))
			.isInstanceOf(AuthenticationFailedException.class)
			.hasMessage("Authentication failed.");
	}

	@Test
	void 비밀번호가_일치하지_않으면_인증에_실패한다() {
		memberRepository.save(activeMember());

		assertThatThrownBy(() -> loginService.login("user01", "wrong-password"))
			.isInstanceOf(AuthenticationFailedException.class)
			.hasMessage("Authentication failed.");
	}

	@Test
	void 비활성_회원이면_인증에_실패한다() {
		memberRepository.save(inactiveMember());

		assertThatThrownBy(() -> loginService.login("user01", "password123"))
			.isInstanceOf(AuthenticationFailedException.class)
			.hasMessage("Authentication failed.");
	}

	private Member activeMember() {
		return new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("encoded-password123"),
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		);
	}

	private Member inactiveMember() {
		return new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("encoded-password123"),
			MemberRole.MEMBER,
			MemberStatus.INACTIVE
		);
	}

	private static class FakeMemberRepository implements MemberRepository {

		private Member member;

		@Override
		public boolean existsByLoginId(LoginId loginId) {
			return false;
		}

		@Override
		public boolean existsByNickname(Nickname nickname) {
			return false;
		}

		@Override
		public Optional<Member> findByLoginId(LoginId loginId) {
			if (member != null && member.loginId().equals(loginId)) {
				return Optional.of(member);
			}
			return Optional.empty();
		}

		@Override
		public Optional<Member> findById(MemberId memberId) {
			if (member != null && member.id().equals(memberId)) {
				return Optional.of(member);
			}
			return Optional.empty();
		}

		@Override
		public MemberId save(Member member) {
			this.member = member;
			return member.id();
		}
	}

	private static class FakePasswordEncoder implements PasswordEncoder {

		@Override
		public String encode(CharSequence rawPassword) {
			return "encoded-" + rawPassword;
		}

		@Override
		public boolean matches(CharSequence rawPassword, String encodedPassword) {
			return encode(rawPassword).equals(encodedPassword);
		}
	}

	private static class FakeAccessTokenIssuer implements AccessTokenIssuer {

		@Override
		public String createAccessToken(MemberId memberId, MemberRole role) {
			return "token-" + memberId.value() + "-" + role.name();
		}
	}
}
```

- [ ] **Step 2: Run login service tests and verify they fail**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "*LoginServiceTest"
```

Expected:

```text
Compilation failed
```

The missing types should include `LoginService` and `AuthenticationFailedException`.

- [ ] **Step 3: Create authentication failure exception**

Create `backend/src/main/java/com/dddblog/backend/auth/application/AuthenticationFailedException.java`:

```java
package com.dddblog.backend.auth.application;

public class AuthenticationFailedException extends RuntimeException {

	public AuthenticationFailedException() {
		super("Authentication failed.");
	}
}
```

- [ ] **Step 4: Implement login service**

Create `backend/src/main/java/com/dddblog/backend/auth/application/LoginService.java`:

```java
package com.dddblog.backend.auth.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberStatus;

@Service
public class LoginService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final AccessTokenIssuer accessTokenIssuer;

	public LoginService(
		MemberRepository memberRepository,
		PasswordEncoder passwordEncoder,
		AccessTokenIssuer accessTokenIssuer
	) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.accessTokenIssuer = accessTokenIssuer;
	}

	public String login(String loginId, String password) {
		Member member = memberRepository.findByLoginId(new LoginId(loginId))
			.orElseThrow(AuthenticationFailedException::new);
		if (!passwordEncoder.matches(password, member.passwordHash().value())) {
			throw new AuthenticationFailedException();
		}
		if (member.status() != MemberStatus.ACTIVE) {
			throw new AuthenticationFailedException();
		}
		return accessTokenIssuer.createAccessToken(member.id(), member.role());
	}
}
```

- [ ] **Step 5: Run login service tests and verify they pass**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "*LoginServiceTest"
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/java/com/dddblog/backend/auth/application backend/src/test/java/com/dddblog/backend/auth/application/LoginServiceTest.java
git commit -m "feat: add login service"
```

---

## Task 5: Add Login API And Authentication Failure Response

**Files:**

- Create: `backend/src/main/java/com/dddblog/backend/auth/api/LoginController.java`
- Create: `backend/src/main/java/com/dddblog/backend/auth/api/LoginRequest.java`
- Create: `backend/src/main/java/com/dddblog/backend/auth/api/LoginResponse.java`
- Create: `backend/src/main/java/com/dddblog/backend/auth/security/JwtAuthenticationEntryPoint.java`
- Modify: `backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java`
- Modify: `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`
- Test: `backend/src/test/java/com/dddblog/backend/auth/api/LoginControllerTest.java`

- [ ] **Step 1: Write failing login controller tests**

Create `backend/src/test/java/com/dddblog/backend/auth/api/LoginControllerTest.java`:

```java
package com.dddblog.backend.auth.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dddblog.backend.auth.application.AuthenticationFailedException;
import com.dddblog.backend.auth.application.LoginService;
import com.dddblog.backend.auth.security.JwtAuthenticationEntryPoint;
import com.dddblog.backend.common.api.GlobalExceptionHandler;
import com.dddblog.backend.config.SecurityConfig;

@WebMvcTest(LoginController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class LoginControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LoginService loginService;

	@Test
	void 로그인에_성공하면_200과_액세스_토큰을_반환한다() throws Exception {
		when(loginService.login(eq("user01"), eq("password123")))
			.thenReturn("access-token");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("access-token"));
	}

	@Test
	void 로그인에_실패하면_401과_오류_메시지를_반환한다() throws Exception {
		when(loginService.login(eq("user01"), eq("password123")))
			.thenThrow(new AuthenticationFailedException());

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Authentication failed."));
	}

	private String validJson() {
		return """
			{
			  "loginId": "user01",
			  "password": "password123"
			}
			""";
	}
}
```

- [ ] **Step 2: Run login controller tests and verify they fail**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "*LoginControllerTest"
```

Expected:

```text
Compilation failed
```

The missing types should include `LoginController`, `LoginRequest`, and `LoginResponse`.

- [ ] **Step 3: Create login DTOs**

Create `backend/src/main/java/com/dddblog/backend/auth/api/LoginRequest.java`:

```java
package com.dddblog.backend.auth.api;

public record LoginRequest(String loginId, String password) {
}
```

Create `backend/src/main/java/com/dddblog/backend/auth/api/LoginResponse.java`:

```java
package com.dddblog.backend.auth.api;

public record LoginResponse(String accessToken) {
}
```

- [ ] **Step 4: Create login controller**

Create `backend/src/main/java/com/dddblog/backend/auth/api/LoginController.java`:

```java
package com.dddblog.backend.auth.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dddblog.backend.auth.application.LoginService;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

	private final LoginService loginService;

	public LoginController(LoginService loginService) {
		this.loginService = loginService;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
		String accessToken = loginService.login(request.loginId(), request.password());
		return ResponseEntity.ok(new LoginResponse(accessToken));
	}
}
```

- [ ] **Step 5: Add authentication entry point**

Create `backend/src/main/java/com/dddblog/backend/auth/security/JwtAuthenticationEntryPoint.java`:

```java
package com.dddblog.backend.auth.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.dddblog.backend.common.api.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException, ServletException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), new ErrorResponse("Authentication failed."));
	}
}
```

- [ ] **Step 6: Map login failure exception to 401**

Modify `backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java`:

```java
package com.dddblog.backend.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dddblog.backend.auth.application.AuthenticationFailedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthenticationFailedException.class)
	ResponseEntity<ErrorResponse> handleAuthenticationFailedException(AuthenticationFailedException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(new ErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
			.body(new ErrorResponse(exception.getMessage()));
	}
}
```

- [ ] **Step 7: Permit login endpoint**

Modify `SecurityConfig.securityFilterChain` authorization block:

```java
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
				.anyRequest().authenticated()
			)
```

- [ ] **Step 8: Run login controller tests and signup controller tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "*LoginControllerTest" --tests "*SignupControllerTest"
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 9: Commit**

```powershell
git add backend/src/main/java/com/dddblog/backend/auth/api backend/src/main/java/com/dddblog/backend/auth/security/JwtAuthenticationEntryPoint.java backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java backend/src/test/java/com/dddblog/backend/auth/api/LoginControllerTest.java
git commit -m "feat: add login api"
```

---

## Task 6: Add JWT Authentication Filter

**Files:**

- Create: `backend/src/main/java/com/dddblog/backend/auth/security/AuthenticatedMember.java`
- Create: `backend/src/main/java/com/dddblog/backend/auth/security/JwtAuthentication.java`
- Create: `backend/src/main/java/com/dddblog/backend/auth/security/JwtAuthenticationFilter.java`
- Modify: `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`

- [ ] **Step 1: Create authenticated member principal**

Create `backend/src/main/java/com/dddblog/backend/auth/security/AuthenticatedMember.java`:

```java
package com.dddblog.backend.auth.security;

import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

public record AuthenticatedMember(MemberId memberId, MemberRole role) {

	public AuthenticatedMember {
		if (memberId == null) {
			throw new IllegalArgumentException("Member id must not be null.");
		}
		if (role == null) {
			throw new IllegalArgumentException("Member role must not be null.");
		}
	}
}
```

- [ ] **Step 2: Create JWT authentication**

Create `backend/src/main/java/com/dddblog/backend/auth/security/JwtAuthentication.java`:

```java
package com.dddblog.backend.auth.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JwtAuthentication extends AbstractAuthenticationToken {

	private final AuthenticatedMember principal;

	public JwtAuthentication(AuthenticatedMember principal) {
		super(authorities(principal));
		this.principal = principal;
		setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return "";
	}

	@Override
	public AuthenticatedMember getPrincipal() {
		return principal;
	}

	private static Collection<? extends GrantedAuthority> authorities(AuthenticatedMember principal) {
		return List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
	}
}
```

- [ ] **Step 3: Create JWT authentication filter**

Create `backend/src/main/java/com/dddblog/backend/auth/security/JwtAuthenticationFilter.java`:

```java
package com.dddblog.backend.auth.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider tokenProvider;
	private final JwtAuthenticationEntryPoint authenticationEntryPoint;

	public JwtAuthenticationFilter(
		JwtTokenProvider tokenProvider,
		JwtAuthenticationEntryPoint authenticationEntryPoint
	) {
		this.tokenProvider = tokenProvider;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			String token = authorization.substring(BEARER_PREFIX.length());
			ParsedAccessToken parsedToken = tokenProvider.parseAccessToken(token);
			AuthenticatedMember authenticatedMember = new AuthenticatedMember(parsedToken.memberId(), parsedToken.role());
			SecurityContextHolder.getContext().setAuthentication(new JwtAuthentication(authenticatedMember));
			filterChain.doFilter(request, response);
		}
		catch (IllegalArgumentException exception) {
			SecurityContextHolder.clearContext();
			authenticationEntryPoint.commence(request, response, null);
		}
	}
}
```

- [ ] **Step 4: Register stateless JWT security**

Modify `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`:

```java
package com.dddblog.backend.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.dddblog.backend.auth.security.JwtAuthenticationEntryPoint;
import com.dddblog.backend.auth.security.JwtAuthenticationFilter;
import com.dddblog.backend.auth.security.JwtProperties;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		JwtAuthenticationFilter jwtAuthenticationFilter,
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
	) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
```

- [ ] **Step 5: Update MVC tests that import SecurityConfig**

Add this import to `backend/src/test/java/com/dddblog/backend/auth/api/LoginControllerTest.java`:

```java
import com.dddblog.backend.auth.security.JwtAuthenticationFilter;
```

Add this field to `LoginControllerTest`:

```java
	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;
```

Add this import to `backend/src/test/java/com/dddblog/backend/member/api/SignupControllerTest.java`:

```java
import com.dddblog.backend.auth.security.JwtAuthenticationFilter;
```

Add this field to `SignupControllerTest`:

```java
	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;
```

- [ ] **Step 6: Run controller tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "*LoginControllerTest" --tests "*SignupControllerTest"
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/dddblog/backend/auth/security backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java backend/src/test/java/com/dddblog/backend/auth/api/LoginControllerTest.java backend/src/test/java/com/dddblog/backend/member/api/SignupControllerTest.java
git commit -m "feat: add jwt authentication filter"
```

---

## Task 7: Add My Info API

**Files:**

- Create: `backend/src/main/java/com/dddblog/backend/member/api/MeController.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/api/MeResponse.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/api/MeService.java`
- Test: `backend/src/test/java/com/dddblog/backend/member/api/MeControllerTest.java`

- [ ] **Step 1: Write failing me controller tests**

Create `backend/src/test/java/com/dddblog/backend/member/api/MeControllerTest.java`:

```java
package com.dddblog.backend.member.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dddblog.backend.auth.security.AuthenticatedMember;
import com.dddblog.backend.auth.security.JwtAuthentication;
import com.dddblog.backend.auth.security.JwtAuthenticationEntryPoint;
import com.dddblog.backend.auth.security.JwtAuthenticationFilter;
import com.dddblog.backend.common.api.GlobalExceptionHandler;
import com.dddblog.backend.config.SecurityConfig;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberRole;

@WebMvcTest(MeController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class MeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MeService meService;

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Test
	void 인증된_요청이면_내_정보를_반환한다() throws Exception {
		when(meService.getMe(any(MemberId.class)))
			.thenReturn(new MeResponse(1L, "홍길동", "길동", "user01", "MEMBER"));

		mockMvc.perform(get("/api/members/me")
				.with(authentication(new JwtAuthentication(
					new AuthenticatedMember(new MemberId(1L), MemberRole.MEMBER)
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.memberId").value(1L))
			.andExpect(jsonPath("$.name").value("홍길동"))
			.andExpect(jsonPath("$.nickname").value("길동"))
			.andExpect(jsonPath("$.loginId").value("user01"))
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void 토큰이_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/members/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Authentication failed."));
	}
}
```

- [ ] **Step 2: Run me controller tests and verify they fail**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "*MeControllerTest"
```

Expected:

```text
Compilation failed
```

Missing types should include `MeController`, `MeService`, and `MeResponse`.

- [ ] **Step 3: Create me response**

Create `backend/src/main/java/com/dddblog/backend/member/api/MeResponse.java`:

```java
package com.dddblog.backend.member.api;

public record MeResponse(
	Long memberId,
	String name,
	String nickname,
	String loginId,
	String role
) {
}
```

- [ ] **Step 4: Create me service**

Create `backend/src/main/java/com/dddblog/backend/member/api/MeService.java`:

```java
package com.dddblog.backend.member.api;

import org.springframework.stereotype.Service;

import com.dddblog.backend.auth.application.AuthenticationFailedException;
import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;

@Service
public class MeService {

	private final MemberRepository memberRepository;

	public MeService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public MeResponse getMe(MemberId memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(AuthenticationFailedException::new);
		return new MeResponse(
			member.id().value(),
			member.name().value(),
			member.nickname().value(),
			member.loginId().value(),
			member.role().name()
		);
	}
}
```

- [ ] **Step 5: Create me controller**

Create `backend/src/main/java/com/dddblog/backend/member/api/MeController.java`:

```java
package com.dddblog.backend.member.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dddblog.backend.auth.security.AuthenticatedMember;

@RestController
@RequestMapping("/api/members")
public class MeController {

	private final MeService meService;

	public MeController(MeService meService) {
		this.meService = meService;
	}

	@GetMapping("/me")
	public ResponseEntity<MeResponse> me(@AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
		return ResponseEntity.ok(meService.getMe(authenticatedMember.memberId()));
	}
}
```

- [ ] **Step 6: Run me controller tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "*MeControllerTest"
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/dddblog/backend/member/api/MeController.java backend/src/main/java/com/dddblog/backend/member/api/MeResponse.java backend/src/main/java/com/dddblog/backend/member/api/MeService.java backend/src/test/java/com/dddblog/backend/member/api/MeControllerTest.java
git commit -m "feat: add current member api"
```

---

## Task 8: Add Login Auth Vertical Integration Test

**Files:**

- Create: `backend/src/test/java/com/dddblog/backend/auth/api/LoginApiIntegrationTest.java`

- [ ] **Step 1: Write vertical integration test**

Create `backend/src/test/java/com/dddblog/backend/auth/api/LoginApiIntegrationTest.java`:

```java
package com.dddblog.backend.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.dddblog.backend.support.MysqlDataJpaTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginApiIntegrationTest extends MysqlDataJpaTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 회원가입_후_로그인하면_발급된_토큰으로_내_정보를_조회할_수_있다() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "홍길동",
					  "nickname": "길동",
					  "loginId": "user01",
					  "password": "password123"
					}
					"""))
			.andExpect(status().isCreated());

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "user01",
					  "password": "password123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isString())
			.andReturn();

		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
			.get("accessToken")
			.asText();

		mockMvc.perform(get("/api/members/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.memberId").isNumber())
			.andExpect(jsonPath("$.name").value("홍길동"))
			.andExpect(jsonPath("$.nickname").value("길동"))
			.andExpect(jsonPath("$.loginId").value("user01"))
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void 잘못된_토큰으로_내_정보를_조회하면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/members/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Authentication failed."));
	}
}
```

- [ ] **Step 2: Run integration test**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests "*LoginApiIntegrationTest"
```

Expected:

```text
BUILD SUCCESSFUL
```

Docker must be running because this test uses MySQL Testcontainers.

- [ ] **Step 3: Commit**

```powershell
git add backend/src/test/java/com/dddblog/backend/auth/api/LoginApiIntegrationTest.java
git commit -m "test: verify login jwt auth flow"
```

---

## Task 9: Refresh Docs And Run Final Verification

**Files:**

- Modify: `docs/handoff.md`

- [ ] **Step 1: Refresh handoff**

Update `docs/handoff.md` to describe:

```markdown
## Current State

- Main workspace: `C:\dev\study\ddd-blog`
- Backend: `C:\dev\study\ddd-blog\backend`
- Java for this project: `C:\java\jdk-21`
- Spring Boot: `3.5.0`
- Latest completed backend slice: login and JWT authentication.

## Implemented Auth Behavior

- `POST /api/auth/login` authenticates by login ID and password.
- Successful login returns `{ "accessToken": "..." }`.
- Access Token is a JWT containing member ID, role, token type, issued-at, and expiration.
- `Authorization: Bearer <token>` authenticates protected requests.
- `GET /api/members/me` returns member ID, name, nickname, login ID, and role.
- Login failure, missing token, invalid token, expired token, and inactive member all return `401 { "message": "Authentication failed." }`.
- Logout is client-side token deletion. There is no server logout endpoint in this slice.

## Exclusions Still True

- No Refresh Token.
- No token reissue.
- No server-side logout or blacklist.
- No member update/delete API.
- No post API integration with authenticated member yet.
```

Preserve still-relevant sections about DDD/TDD rules, Testcontainers, and known Hibernate schema-drop noise.

- [ ] **Step 2: Run full backend tests**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 3: Check test naming rule**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected: no output.

- [ ] **Step 4: Check pure package annotation rule**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional|@Configuration|@Bean'
```

Expected: no output.

- [ ] **Step 5: Check H2 was not reintroduced**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
rg -n "h2database|jdbc:h2|H2Dialect|com\.h2database" .
```

Expected: no matches. `rg` exits with code `1` when there are no matches.

- [ ] **Step 6: Check whitespace**

Run:

```powershell
cd C:\dev\study\ddd-blog
git diff --check
```

Expected: no output.

- [ ] **Step 7: Commit**

```powershell
git add docs/handoff.md
git commit -m "docs: update handoff after login jwt auth"
```

---

## Final Review Checklist

- [ ] `POST /api/auth/signup` still works without authentication.
- [ ] `POST /api/auth/login` works without authentication.
- [ ] `GET /api/members/me` requires authentication.
- [ ] Valid JWT authenticates requests.
- [ ] Invalid JWT returns `401 {"message":"Authentication failed."}`.
- [ ] Missing JWT on protected endpoint returns `401 {"message":"Authentication failed."}`.
- [ ] Login failure does not reveal whether login ID exists.
- [ ] JWT does not contain name, nickname, or login ID.
- [ ] `/api/members/me` does not return password hash.
- [ ] Refresh Token and server logout were not added.
- [ ] `member.domain` and `member.application` stayed free of Spring/JPA annotations.
- [ ] Full backend test suite passes.
