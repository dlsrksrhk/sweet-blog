# Signup API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first `POST /api/auth/signup` backend API slice that validates a raw password, hashes it with BCrypt, delegates to the existing pure member registration application service, persists the member, and returns the generated member ID.

**Architecture:** Keep `member.domain` and `member.application` pure. Add a thin Spring API facade in `member.api`, put Spring configuration outside the pure application package, and expose minimal shared error handling through a common API package. Use MySQL Testcontainers only for the final vertical integration test.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring MVC, Spring Security, BCrypt `PasswordEncoder`, JUnit 5, AssertJ, MockMvc, Mockito, MySQL Testcontainers.

---

## File Structure

Create these production files:

```text
backend/src/main/java/com/dddblog/backend/member/domain/RawPassword.java
backend/src/main/java/com/dddblog/backend/member/api/SignupRequest.java
backend/src/main/java/com/dddblog/backend/member/api/SignupResponse.java
backend/src/main/java/com/dddblog/backend/member/api/SignupService.java
backend/src/main/java/com/dddblog/backend/member/api/SignupController.java
backend/src/main/java/com/dddblog/backend/member/config/MemberApplicationConfig.java
backend/src/main/java/com/dddblog/backend/config/PasswordConfig.java
backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java
backend/src/main/java/com/dddblog/backend/common/api/ErrorResponse.java
backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java
```

Create these test files:

```text
backend/src/test/java/com/dddblog/backend/member/domain/RawPasswordTest.java
backend/src/test/java/com/dddblog/backend/member/api/SignupServiceTest.java
backend/src/test/java/com/dddblog/backend/member/api/SignupControllerTest.java
backend/src/test/java/com/dddblog/backend/member/api/SignupApiIntegrationTest.java
```

Modify this documentation file at the end:

```text
docs/handoff.md
```

Keep these boundaries:

- Do not add Spring or JPA annotations to `member.domain` or `member.application`.
- Do not modify `RegisterMemberService` unless a compile-time need appears during implementation review.
- Do not add login, JWT, refresh token, member read APIs, field-level error responses, or OpenAPI docs.
- Do not use ASCII test method names in backend tests.

---

### Task 1: Add RawPassword Domain Value Object

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/RawPassword.java`
- Test: `backend/src/test/java/com/dddblog/backend/member/domain/RawPasswordTest.java`

- [ ] **Step 1: Write the failing RawPassword tests**

Create `backend/src/test/java/com/dddblog/backend/member/domain/RawPasswordTest.java`:

```java
package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RawPasswordTest {

	@Test
	void 비밀번호가_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new RawPassword(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password must not be blank.");
	}

	@Test
	void 비밀번호가_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new RawPassword(" "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password must not be blank.");
	}

	@Test
	void 비밀번호가_8자_미만이면_생성할_수_없다() {
		assertThatThrownBy(() -> new RawPassword("1234567"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password must be at least 8 characters.");
	}

	@Test
	void 유효한_비밀번호이면_원문_값을_반환한다() {
		RawPassword password = new RawPassword("password123");

		assertThat(password.value()).isEqualTo("password123");
	}
}
```

- [ ] **Step 2: Run the RawPassword tests and verify RED**

Run from `backend`:

```powershell
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.member.domain.RawPasswordTest
```

Expected: `RawPassword` compile error because the class does not exist.

- [ ] **Step 3: Implement RawPassword minimally**

Create `backend/src/main/java/com/dddblog/backend/member/domain/RawPassword.java`:

```java
package com.dddblog.backend.member.domain;

public record RawPassword(String value) {

	public RawPassword {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Password must not be blank.");
		}
		if (value.length() < 8) {
			throw new IllegalArgumentException("Password must be at least 8 characters.");
		}
	}
}
```

- [ ] **Step 4: Run the RawPassword tests and verify GREEN**

Run:

```powershell
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.member.domain.RawPasswordTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit Task 1**

Run from repository root:

```powershell
git add backend/src/main/java/com/dddblog/backend/member/domain/RawPassword.java backend/src/test/java/com/dddblog/backend/member/domain/RawPasswordTest.java
git commit -m "feat: add raw password value object"
```

---

### Task 2: Add Signup DTOs And API Facade Service

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/member/api/SignupRequest.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/api/SignupResponse.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/api/SignupService.java`
- Test: `backend/src/test/java/com/dddblog/backend/member/api/SignupServiceTest.java`

- [ ] **Step 1: Write the failing SignupService tests**

Create `backend/src/test/java/com/dddblog/backend/member/api/SignupServiceTest.java`:

```java
package com.dddblog.backend.member.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dddblog.backend.member.application.MemberIdGenerator;
import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.application.RegisterMemberService;
import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

class SignupServiceTest {

	@Test
	void 회원가입을_요청하면_비밀번호를_해시해서_회원가입_서비스에_전달한다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		FakePasswordEncoder passwordEncoder = new FakePasswordEncoder();
		RegisterMemberService registerMemberService = new RegisterMemberService(memberRepository, memberIdGenerator);
		SignupService signupService = new SignupService(registerMemberService, passwordEncoder);

		signupService.signup(validRequest());

		Member savedMember = memberRepository.savedMembers().get(0);
		assertThat(passwordEncoder.encodedPasswords()).containsExactly("password123");
		assertThat(savedMember.passwordHash()).isEqualTo(new PasswordHash("$2a$10$encoded-password"));
	}

	@Test
	void 회원가입에_성공하면_회원_ID를_반환한다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		RegisterMemberService registerMemberService = new RegisterMemberService(memberRepository, memberIdGenerator);
		SignupService signupService = new SignupService(registerMemberService, new FakePasswordEncoder());

		SignupResponse response = signupService.signup(validRequest());

		assertThat(response.memberId()).isEqualTo(1L);
	}

	@Test
	void 비밀번호가_8자_미만이면_회원가입_서비스를_호출하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		FakeMemberIdGenerator memberIdGenerator = new FakeMemberIdGenerator();
		FakePasswordEncoder passwordEncoder = new FakePasswordEncoder();
		RegisterMemberService registerMemberService = new RegisterMemberService(memberRepository, memberIdGenerator);
		SignupService signupService = new SignupService(registerMemberService, passwordEncoder);
		SignupRequest request = new SignupRequest("홍길동", "길동", "user01", "1234567");

		assertThatThrownBy(() -> signupService.signup(request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password must be at least 8 characters.");
		assertThat(passwordEncoder.encodedPasswords()).isEmpty();
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	private SignupRequest validRequest() {
		return new SignupRequest("홍길동", "길동", "user01", "password123");
	}

	private static class FakePasswordEncoder implements PasswordEncoder {

		private final List<String> encodedPasswords = new ArrayList<>();

		@Override
		public String encode(CharSequence rawPassword) {
			encodedPasswords.add(rawPassword.toString());
			return "$2a$10$encoded-password";
		}

		@Override
		public boolean matches(CharSequence rawPassword, String encodedPassword) {
			return "$2a$10$encoded-password".equals(encodedPassword);
		}

		List<String> encodedPasswords() {
			return List.copyOf(encodedPasswords);
		}
	}

	private static class FakeMemberRepository implements MemberRepository {

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
		public MemberId save(Member member) {
			savedMembers.add(member);
			existingLoginIds.add(member.loginId());
			existingNicknames.add(member.nickname());
			return member.id();
		}

		List<Member> savedMembers() {
			return List.copyOf(savedMembers);
		}
	}

	private static class FakeMemberIdGenerator implements MemberIdGenerator {

		private long nextId = 1L;

		@Override
		public MemberId nextId() {
			return new MemberId(nextId++);
		}
	}
}
```

- [ ] **Step 2: Run the SignupService tests and verify RED**

Run from `backend`:

```powershell
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.member.api.SignupServiceTest
```

Expected: compile error because `SignupRequest`, `SignupResponse`, and `SignupService` do not exist.

- [ ] **Step 3: Add SignupRequest**

Create `backend/src/main/java/com/dddblog/backend/member/api/SignupRequest.java`:

```java
package com.dddblog.backend.member.api;

public record SignupRequest(
	String name,
	String nickname,
	String loginId,
	String password
) {
}
```

- [ ] **Step 4: Add SignupResponse**

Create `backend/src/main/java/com/dddblog/backend/member/api/SignupResponse.java`:

```java
package com.dddblog.backend.member.api;

public record SignupResponse(Long memberId) {
}
```

- [ ] **Step 5: Add SignupService**

Create `backend/src/main/java/com/dddblog/backend/member/api/SignupService.java`:

```java
package com.dddblog.backend.member.api;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dddblog.backend.member.application.RegisterMemberCommand;
import com.dddblog.backend.member.application.RegisterMemberService;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.RawPassword;

@Service
public class SignupService {

	private final RegisterMemberService registerMemberService;
	private final PasswordEncoder passwordEncoder;

	public SignupService(RegisterMemberService registerMemberService, PasswordEncoder passwordEncoder) {
		this.registerMemberService = registerMemberService;
		this.passwordEncoder = passwordEncoder;
	}

	public SignupResponse signup(SignupRequest request) {
		RawPassword rawPassword = new RawPassword(request.password());
		String passwordHash = passwordEncoder.encode(rawPassword.value());
		RegisterMemberCommand command = new RegisterMemberCommand(
			request.name(),
			request.nickname(),
			request.loginId(),
			passwordHash
		);
		MemberId memberId = registerMemberService.register(command);

		return new SignupResponse(memberId.value());
	}
}
```

- [ ] **Step 6: Run the SignupService tests and verify GREEN**

Run:

```powershell
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.member.api.SignupServiceTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit Task 2**

Run from repository root:

```powershell
git add backend/src/main/java/com/dddblog/backend/member/api/SignupRequest.java backend/src/main/java/com/dddblog/backend/member/api/SignupResponse.java backend/src/main/java/com/dddblog/backend/member/api/SignupService.java backend/src/test/java/com/dddblog/backend/member/api/SignupServiceTest.java
git commit -m "feat: add signup api facade service"
```

---

### Task 3: Add Spring Configuration For Member Application, Password, And Security

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/member/config/MemberApplicationConfig.java`
- Create: `backend/src/main/java/com/dddblog/backend/config/PasswordConfig.java`
- Create: `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`

- [ ] **Step 1: Add MemberApplicationConfig**

Create `backend/src/main/java/com/dddblog/backend/member/config/MemberApplicationConfig.java`:

```java
package com.dddblog.backend.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dddblog.backend.member.application.MemberIdGenerator;
import com.dddblog.backend.member.application.MemberRepository;
import com.dddblog.backend.member.application.RegisterMemberService;

@Configuration
public class MemberApplicationConfig {

	@Bean
	RegisterMemberService registerMemberService(
		MemberRepository memberRepository,
		MemberIdGenerator memberIdGenerator
	) {
		return new RegisterMemberService(memberRepository, memberIdGenerator);
	}
}
```

- [ ] **Step 2: Add PasswordConfig**

Create `backend/src/main/java/com/dddblog/backend/config/PasswordConfig.java`:

```java
package com.dddblog.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
```

- [ ] **Step 3: Add SecurityConfig**

Create `backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java`:

```java
package com.dddblog.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
				.anyRequest().authenticated()
			)
			.httpBasic(Customizer.withDefaults())
			.build();
	}
}
```

- [ ] **Step 4: Run compile tests for configuration**

Run from `backend`:

```powershell
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.member.api.SignupServiceTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Verify pure application package still has no Spring annotations**

Run from `backend`:

```powershell
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional|@Configuration|@Bean'
```

Expected: no output.

- [ ] **Step 6: Commit Task 3**

Run from repository root:

```powershell
git add backend/src/main/java/com/dddblog/backend/member/config/MemberApplicationConfig.java backend/src/main/java/com/dddblog/backend/config/PasswordConfig.java backend/src/main/java/com/dddblog/backend/config/SecurityConfig.java
git commit -m "feat: configure signup dependencies"
```

---

### Task 4: Add Signup Controller And Minimal Error Response

**Files:**
- Create: `backend/src/main/java/com/dddblog/backend/member/api/SignupController.java`
- Create: `backend/src/main/java/com/dddblog/backend/common/api/ErrorResponse.java`
- Create: `backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/dddblog/backend/member/api/SignupControllerTest.java`

- [ ] **Step 1: Write the failing SignupController tests**

Create `backend/src/test/java/com/dddblog/backend/member/api/SignupControllerTest.java`:

```java
package com.dddblog.backend.member.api;

import static org.mockito.ArgumentMatchers.any;
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

import com.dddblog.backend.common.api.GlobalExceptionHandler;
import com.dddblog.backend.config.SecurityConfig;

@WebMvcTest(SignupController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class SignupControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SignupService signupService;

	@Test
	void 회원가입에_성공하면_201과_회원_ID를_반환한다() throws Exception {
		when(signupService.signup(any(SignupRequest.class)))
			.thenReturn(new SignupResponse(1L));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId").value(1L));
	}

	@Test
	void 회원가입_요청이_실패하면_400과_오류_메시지를_반환한다() throws Exception {
		when(signupService.signup(any(SignupRequest.class)))
			.thenThrow(new IllegalArgumentException("Login id already exists."));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Login id already exists."));
	}

	@Test
	void 인증_없이_회원가입을_요청할_수_있다() throws Exception {
		when(signupService.signup(any(SignupRequest.class)))
			.thenReturn(new SignupResponse(1L));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
			.andExpect(status().isCreated());
	}

	private String validJson() {
		return """
			{
			  "name": "홍길동",
			  "nickname": "길동",
			  "loginId": "user01",
			  "password": "password123"
			}
			""";
	}
}
```

- [ ] **Step 2: Run the SignupController tests and verify RED**

Run from `backend`:

```powershell
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.member.api.SignupControllerTest
```

Expected: compile error because `SignupController`, `GlobalExceptionHandler`, and `ErrorResponse` do not exist.

- [ ] **Step 3: Add SignupController**

Create `backend/src/main/java/com/dddblog/backend/member/api/SignupController.java`:

```java
package com.dddblog.backend.member.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SignupController {

	private final SignupService signupService;

	public SignupController(SignupService signupService) {
		this.signupService = signupService;
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public SignupResponse signup(@RequestBody SignupRequest request) {
		return signupService.signup(request);
	}
}
```

- [ ] **Step 4: Add ErrorResponse**

Create `backend/src/main/java/com/dddblog/backend/common/api/ErrorResponse.java`:

```java
package com.dddblog.backend.common.api;

public record ErrorResponse(String message) {
}
```

- [ ] **Step 5: Add GlobalExceptionHandler**

Create `backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java`:

```java
package com.dddblog.backend.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleIllegalArgumentException(IllegalArgumentException exception) {
		return new ErrorResponse(exception.getMessage());
	}
}
```

- [ ] **Step 6: Run the SignupController tests and verify GREEN**

Run:

```powershell
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.member.api.SignupControllerTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit Task 4**

Run from repository root:

```powershell
git add backend/src/main/java/com/dddblog/backend/member/api/SignupController.java backend/src/main/java/com/dddblog/backend/common/api/ErrorResponse.java backend/src/main/java/com/dddblog/backend/common/api/GlobalExceptionHandler.java backend/src/test/java/com/dddblog/backend/member/api/SignupControllerTest.java
git commit -m "feat: add signup controller"
```

---

### Task 5: Add Vertical Signup API Integration Test

**Files:**
- Test: `backend/src/test/java/com/dddblog/backend/member/api/SignupApiIntegrationTest.java`

- [ ] **Step 1: Write the failing vertical integration test**

Create `backend/src/test/java/com/dddblog/backend/member/api/SignupApiIntegrationTest.java`:

```java
package com.dddblog.backend.member.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.dddblog.backend.support.MysqlDataJpaTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SignupApiIntegrationTest extends MysqlDataJpaTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void 회원가입_API는_BCrypt로_해시한_비밀번호를_members에_저장한다() throws Exception {
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
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId").isNumber());

		String passwordHash = jdbcTemplate.queryForObject(
			"select password_hash from members where login_id = ?",
			String.class,
			"user01"
		);
		assertThat(passwordHash).isNotEqualTo("password123");
		assertThat(new BCryptPasswordEncoder().matches("password123", passwordHash)).isTrue();
	}
}
```

- [ ] **Step 2: Run the vertical integration test**

Run from `backend`:

```powershell
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.member.api.SignupApiIntegrationTest
```

Expected: `BUILD SUCCESSFUL`.

If this test fails, use `superpowers:systematic-debugging` before changing code. The expected design after debugging must still be:

- `MemberApplicationConfig` provides `RegisterMemberService`.
- `PasswordConfig` provides `PasswordEncoder`.
- `SecurityConfig` permits unauthenticated `POST /api/auth/signup`.
- CSRF does not block `POST /api/auth/signup`.
- The integration test reaches the real JPA member persistence adapter and stores a BCrypt hash.

- [ ] **Step 3: Run the signup-related test set**

Run:

```powershell
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --tests com.dddblog.backend.member.domain.RawPasswordTest --tests com.dddblog.backend.member.api.SignupServiceTest --tests com.dddblog.backend.member.api.SignupControllerTest --tests com.dddblog.backend.member.api.SignupApiIntegrationTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit Task 5**

Run from repository root:

```powershell
git add backend/src/test/java/com/dddblog/backend/member/api/SignupApiIntegrationTest.java
git commit -m "test: verify signup api vertical slice"
```

---

### Task 6: Final Verification And Handoff Refresh

**Files:**
- Modify: `docs/handoff.md`

- [ ] **Step 1: Run the full backend test suite**

Run from `backend`:

```powershell
$env:JAVA_HOME='C:\java\jdk-21'
.\gradlew.bat test --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`.

Known acceptable noise: Hibernate may log non-blocking schema-drop errors during shutdown while using shared MySQL Testcontainers and `ddl-auto=create-drop`. The Gradle result must still be successful.

- [ ] **Step 2: Verify backend test method naming**

Run from `backend`:

```powershell
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected: no output.

- [ ] **Step 3: Verify pure domain/application packages remain annotation-free**

Run from `backend`:

```powershell
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\blog\domain,.\src\main\java\com\dddblog\backend\blog\application,.\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional|@Configuration|@Bean'
```

Expected: no output.

- [ ] **Step 4: Verify H2 was not reintroduced**

Run from `backend`:

```powershell
rg -n "h2database|jdbc:h2|H2Dialect|com\.h2database" .
```

Expected: no matches. `rg` exits with code `1` when there are no matches; that is acceptable for this check.

- [ ] **Step 5: Refresh docs/handoff.md**

Update `docs/handoff.md` so it reflects the completed signup API slice.

Add or update these facts:

````markdown
### Member API Layer

Package:

```text
backend/src/main/java/com/dddblog/backend/member/api
```

Implemented:

- `SignupController`
- `SignupRequest`
- `SignupResponse`
- `SignupService`

Behavior:

- `POST /api/auth/signup` accepts `name`, `nickname`, `loginId`, and raw `password`.
- `SignupService` validates the raw password with `RawPassword`.
- `SignupService` hashes the password with BCrypt before calling `RegisterMemberService`.
- Successful signup returns `201 Created` with `memberId`.
- `IllegalArgumentException` is returned as `400 Bad Request` with `{ "message": "..." }`.
````

Also update the current state, recent commits, verification commands, and next-step notes so they no longer say the signup API is pending.

- [ ] **Step 6: Commit Task 6**

Run from repository root:

```powershell
git add docs/handoff.md
git commit -m "docs: update handoff after signup api"
```

- [ ] **Step 7: Final review before branch finishing**

Use a final code review pass over the branch.

Review checklist:

- `RawPassword` covers null, blank, short, and valid inputs.
- `SignupService` hashes raw password before building `RegisterMemberCommand`.
- `RegisterMemberService` remains annotation-free.
- `member.domain` and `member.application` remain free of Spring/JPA annotations.
- `POST /api/auth/signup` is unauthenticated.
- `IllegalArgumentException` returns `400` and `{ "message": ... }`.
- The vertical integration test proves BCrypt hash is stored in `members`.
- No login/JWT/read API work slipped into this slice.

If the review finds issues, fix them with TDD and rerun the relevant verification before finishing the branch.
