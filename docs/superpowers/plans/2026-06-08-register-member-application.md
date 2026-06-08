# Register Member Application Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first pure Java member registration application service that validates a registration command, checks duplicate login ID and nickname through a repository port, saves a `Member`, and returns `MemberId`.

**Architecture:** Add a focused `com.dddblog.backend.member.application` package with a command record, service, and repository port. Keep the service Spring-free and JPA-free; tests use a package-private fake repository and do not start Spring Context. The repository port owns ID generation through `nextId()`, while `Member` keeps its existing rule that an ID is required.

**Tech Stack:** Java 21, JUnit 5, AssertJ, Gradle, existing Spring Boot test setup.

---

## File Structure

- Create: `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberCommand.java`
  - Record containing primitive registration request values.
- Create: `backend/src/main/java/com/dddblog/backend/member/application/MemberRepository.java`
  - Application port used by `RegisterMemberService`.
- Create: `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberService.java`
  - Pure Java use case service for member registration.
- Create: `backend/src/test/java/com/dddblog/backend/member/application/RegisterMemberServiceTest.java`
  - Spring-free unit tests for the registration use case.
- Create: `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberRepository.java`
  - Test fake implementing `MemberRepository`.

Do not modify existing `member.domain`, `blog`, persistence, API, security, or Gradle files in this plan.

---

### Task 1: Register Valid Member

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/member/application/RegisterMemberServiceTest.java`
- Create: `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberRepository.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberCommand.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/application/MemberRepository.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberService.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/dddblog/backend/member/application/RegisterMemberServiceTest.java`:

```java
package com.dddblog.backend.member.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.member.domain.MemberId;

class RegisterMemberServiceTest {

	@Test
	void 유효한_요청이면_회원을_저장하고_ID를_반환한다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		MemberId memberId = service.register(command);

		assertThat(memberId).isEqualTo(new MemberId(1L));
		assertThat(memberRepository.savedMembers()).hasSize(1);
	}

	private RegisterMemberCommand validCommand() {
		return new RegisterMemberCommand(
			"홍길동",
			"길동",
			"user01",
			"$2a$10$hashed-password"
		);
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.member.application.RegisterMemberServiceTest
```

Expected: FAIL because `FakeMemberRepository`, `RegisterMemberService`, and `RegisterMemberCommand` do not exist.

- [ ] **Step 3: Write minimal implementation**

Create `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberCommand.java`:

```java
package com.dddblog.backend.member.application;

public record RegisterMemberCommand(
	String name,
	String nickname,
	String loginId,
	String passwordHash
) {
}
```

Create `backend/src/main/java/com/dddblog/backend/member/application/MemberRepository.java`:

```java
package com.dddblog.backend.member.application;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;

public interface MemberRepository {

	boolean existsByLoginId(LoginId loginId);

	boolean existsByNickname(Nickname nickname);

	MemberId nextId();

	MemberId save(Member member);
}
```

Create `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberService.java`:

```java
package com.dddblog.backend.member.application;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

public class RegisterMemberService {

	private final MemberRepository memberRepository;

	public RegisterMemberService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public MemberId register(RegisterMemberCommand command) {
		MemberId memberId = memberRepository.nextId();
		Member member = Member.register(
			memberId,
			new MemberName(command.name()),
			new Nickname(command.nickname()),
			new LoginId(command.loginId()),
			new PasswordHash(command.passwordHash())
		);
		return memberRepository.save(member);
	}
}
```

Create `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberRepository.java`:

```java
package com.dddblog.backend.member.application;

import java.util.ArrayList;
import java.util.List;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;

class FakeMemberRepository implements MemberRepository {

	private final List<Member> savedMembers = new ArrayList<>();
	private long nextId = 1L;

	@Override
	public boolean existsByLoginId(LoginId loginId) {
		return false;
	}

	@Override
	public boolean existsByNickname(Nickname nickname) {
		return false;
	}

	@Override
	public MemberId nextId() {
		return new MemberId(nextId++);
	}

	@Override
	public MemberId save(Member member) {
		savedMembers.add(member);
		return member.id();
	}

	List<Member> savedMembers() {
		return List.copyOf(savedMembers);
	}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
.\gradlew.bat test --tests com.dddblog.backend.member.application.RegisterMemberServiceTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run from repository root:

```powershell
cd C:\dev\study\ddd-blog
git add backend\src\test\java\com\dddblog\backend\member\application\RegisterMemberServiceTest.java backend\src\test\java\com\dddblog\backend\member\application\FakeMemberRepository.java backend\src\main\java\com\dddblog\backend\member\application\RegisterMemberCommand.java backend\src\main\java\com\dddblog\backend\member\application\MemberRepository.java backend\src\main\java\com\dddblog\backend\member\application\RegisterMemberService.java
git commit -m "feat: add member registration service"
```

Expected: commit succeeds.

---

### Task 2: Verify Saved Member Values

**Files:**
- Modify: `backend/src/test/java/com/dddblog/backend/member/application/RegisterMemberServiceTest.java`

- [ ] **Step 1: Write the failing tests**

Replace `backend/src/test/java/com/dddblog/backend/member/application/RegisterMemberServiceTest.java` with:

```java
package com.dddblog.backend.member.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.MemberRole;
import com.dddblog.backend.member.domain.MemberStatus;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

class RegisterMemberServiceTest {

	@Test
	void 유효한_요청이면_회원을_저장하고_ID를_반환한다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		MemberId memberId = service.register(command);

		assertThat(memberId).isEqualTo(new MemberId(1L));
		assertThat(memberRepository.savedMembers()).hasSize(1);
	}

	@Test
	void 저장된_회원은_요청_값을_도메인_값으로_가진다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		service.register(command);

		Member savedMember = memberRepository.savedMembers().get(0);
		assertThat(savedMember.id()).isEqualTo(new MemberId(1L));
		assertThat(savedMember.name()).isEqualTo(new MemberName("홍길동"));
		assertThat(savedMember.nickname()).isEqualTo(new Nickname("길동"));
		assertThat(savedMember.loginId()).isEqualTo(new LoginId("user01"));
		assertThat(savedMember.passwordHash()).isEqualTo(new PasswordHash("$2a$10$hashed-password"));
	}

	@Test
	void 신규_회원은_MEMBER_권한과_ACTIVE_상태를_가진다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		service.register(command);

		Member savedMember = memberRepository.savedMembers().get(0);
		assertThat(savedMember.role()).isEqualTo(MemberRole.MEMBER);
		assertThat(savedMember.status()).isEqualTo(MemberStatus.ACTIVE);
	}

	private RegisterMemberCommand validCommand() {
		return new RegisterMemberCommand(
			"홍길동",
			"길동",
			"user01",
			"$2a$10$hashed-password"
		);
	}
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.member.application.RegisterMemberServiceTest
```

Expected: PASS. These tests document existing behavior produced by Task 1.

- [ ] **Step 3: Commit**

Run from repository root:

```powershell
cd C:\dev\study\ddd-blog
git add backend\src\test\java\com\dddblog\backend\member\application\RegisterMemberServiceTest.java
git commit -m "test: verify registered member values"
```

Expected: commit succeeds.

---

### Task 3: Reject Null Command And Invalid Values

**Files:**
- Modify: `backend/src/test/java/com/dddblog/backend/member/application/RegisterMemberServiceTest.java`
- Modify: `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberService.java`

- [ ] **Step 1: Write the failing tests**

Replace `backend/src/test/java/com/dddblog/backend/member/application/RegisterMemberServiceTest.java` with:

```java
package com.dddblog.backend.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.MemberRole;
import com.dddblog.backend.member.domain.MemberStatus;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

class RegisterMemberServiceTest {

	@Test
	void 유효한_요청이면_회원을_저장하고_ID를_반환한다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		MemberId memberId = service.register(command);

		assertThat(memberId).isEqualTo(new MemberId(1L));
		assertThat(memberRepository.savedMembers()).hasSize(1);
	}

	@Test
	void 저장된_회원은_요청_값을_도메인_값으로_가진다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		service.register(command);

		Member savedMember = memberRepository.savedMembers().get(0);
		assertThat(savedMember.id()).isEqualTo(new MemberId(1L));
		assertThat(savedMember.name()).isEqualTo(new MemberName("홍길동"));
		assertThat(savedMember.nickname()).isEqualTo(new Nickname("길동"));
		assertThat(savedMember.loginId()).isEqualTo(new LoginId("user01"));
		assertThat(savedMember.passwordHash()).isEqualTo(new PasswordHash("$2a$10$hashed-password"));
	}

	@Test
	void 신규_회원은_MEMBER_권한과_ACTIVE_상태를_가진다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		service.register(command);

		Member savedMember = memberRepository.savedMembers().get(0);
		assertThat(savedMember.role()).isEqualTo(MemberRole.MEMBER);
		assertThat(savedMember.status()).isEqualTo(MemberStatus.ACTIVE);
	}

	@Test
	void command가_null이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);

		assertThatThrownBy(() -> service.register(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Register member command must not be null.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 잘못된_로그인_ID이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = new RegisterMemberCommand(
			"홍길동",
			"길동",
			"abc",
			"$2a$10$hashed-password"
		);

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id must be 4 characters or more.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 잘못된_닉네임이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = new RegisterMemberCommand(
			"홍길동",
			"길",
			"user01",
			"$2a$10$hashed-password"
		);

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname must be 2 characters or more.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 잘못된_비밀번호_해시이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = new RegisterMemberCommand(
			"홍길동",
			"길동",
			"user01",
			" "
		);

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password hash must not be blank.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	private RegisterMemberCommand validCommand() {
		return new RegisterMemberCommand(
			"홍길동",
			"길동",
			"user01",
			"$2a$10$hashed-password"
		);
	}
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.member.application.RegisterMemberServiceTest
```

Expected: FAIL because null command currently causes a `NullPointerException` instead of `IllegalArgumentException`.

- [ ] **Step 3: Write minimal implementation**

Replace `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberService.java` with:

```java
package com.dddblog.backend.member.application;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

public class RegisterMemberService {

	private final MemberRepository memberRepository;

	public RegisterMemberService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public MemberId register(RegisterMemberCommand command) {
		if (command == null) {
			throw new IllegalArgumentException("Register member command must not be null.");
		}
		MemberId memberId = memberRepository.nextId();
		Member member = Member.register(
			memberId,
			new MemberName(command.name()),
			new Nickname(command.nickname()),
			new LoginId(command.loginId()),
			new PasswordHash(command.passwordHash())
		);
		return memberRepository.save(member);
	}
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat test --tests com.dddblog.backend.member.application.RegisterMemberServiceTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run from repository root:

```powershell
cd C:\dev\study\ddd-blog
git add backend\src\test\java\com\dddblog\backend\member\application\RegisterMemberServiceTest.java backend\src\main\java\com\dddblog\backend\member\application\RegisterMemberService.java
git commit -m "test: reject invalid member registration command"
```

Expected: commit succeeds.

---

### Task 4: Reject Duplicate Login ID And Nickname

**Files:**
- Modify: `backend/src/test/java/com/dddblog/backend/member/application/RegisterMemberServiceTest.java`
- Modify: `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberRepository.java`
- Modify: `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberService.java`

- [ ] **Step 1: Write the failing tests**

Replace `backend/src/test/java/com/dddblog/backend/member/application/RegisterMemberServiceTest.java` with:

```java
package com.dddblog.backend.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.MemberRole;
import com.dddblog.backend.member.domain.MemberStatus;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

class RegisterMemberServiceTest {

	@Test
	void 유효한_요청이면_회원을_저장하고_ID를_반환한다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		MemberId memberId = service.register(command);

		assertThat(memberId).isEqualTo(new MemberId(1L));
		assertThat(memberRepository.savedMembers()).hasSize(1);
	}

	@Test
	void 저장된_회원은_요청_값을_도메인_값으로_가진다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		service.register(command);

		Member savedMember = memberRepository.savedMembers().get(0);
		assertThat(savedMember.id()).isEqualTo(new MemberId(1L));
		assertThat(savedMember.name()).isEqualTo(new MemberName("홍길동"));
		assertThat(savedMember.nickname()).isEqualTo(new Nickname("길동"));
		assertThat(savedMember.loginId()).isEqualTo(new LoginId("user01"));
		assertThat(savedMember.passwordHash()).isEqualTo(new PasswordHash("$2a$10$hashed-password"));
	}

	@Test
	void 신규_회원은_MEMBER_권한과_ACTIVE_상태를_가진다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		service.register(command);

		Member savedMember = memberRepository.savedMembers().get(0);
		assertThat(savedMember.role()).isEqualTo(MemberRole.MEMBER);
		assertThat(savedMember.status()).isEqualTo(MemberStatus.ACTIVE);
	}

	@Test
	void command가_null이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);

		assertThatThrownBy(() -> service.register(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Register member command must not be null.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 로그인_ID가_이미_존재하면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		memberRepository.addExistingLoginId(new LoginId("user01"));
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id already exists.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 닉네임이_이미_존재하면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		memberRepository.addExistingNickname(new Nickname("길동"));
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = validCommand();

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname already exists.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 잘못된_로그인_ID이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = new RegisterMemberCommand(
			"홍길동",
			"길동",
			"abc",
			"$2a$10$hashed-password"
		);

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id must be 4 characters or more.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 잘못된_닉네임이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = new RegisterMemberCommand(
			"홍길동",
			"길",
			"user01",
			"$2a$10$hashed-password"
		);

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname must be 2 characters or more.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	@Test
	void 잘못된_비밀번호_해시이면_저장하지_않는다() {
		FakeMemberRepository memberRepository = new FakeMemberRepository();
		RegisterMemberService service = new RegisterMemberService(memberRepository);
		RegisterMemberCommand command = new RegisterMemberCommand(
			"홍길동",
			"길동",
			"user01",
			" "
		);

		assertThatThrownBy(() -> service.register(command))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password hash must not be blank.");
		assertThat(memberRepository.savedMembers()).isEmpty();
	}

	private RegisterMemberCommand validCommand() {
		return new RegisterMemberCommand(
			"홍길동",
			"길동",
			"user01",
			"$2a$10$hashed-password"
		);
	}
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.member.application.RegisterMemberServiceTest
```

Expected: FAIL because `FakeMemberRepository.addExistingLoginId(...)` and `addExistingNickname(...)` do not exist.

- [ ] **Step 3: Update fake repository**

Replace `backend/src/test/java/com/dddblog/backend/member/application/FakeMemberRepository.java` with:

```java
package com.dddblog.backend.member.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.Nickname;

class FakeMemberRepository implements MemberRepository {

	private final List<Member> savedMembers = new ArrayList<>();
	private final Set<LoginId> existingLoginIds = new HashSet<>();
	private final Set<Nickname> existingNicknames = new HashSet<>();
	private long nextId = 1L;

	@Override
	public boolean existsByLoginId(LoginId loginId) {
		return existingLoginIds.contains(loginId);
	}

	@Override
	public boolean existsByNickname(Nickname nickname) {
		return existingNicknames.contains(nickname);
	}

	@Override
	public MemberId nextId() {
		return new MemberId(nextId++);
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

	void addExistingLoginId(LoginId loginId) {
		existingLoginIds.add(loginId);
	}

	void addExistingNickname(Nickname nickname) {
		existingNicknames.add(nickname);
	}
}
```

- [ ] **Step 4: Run tests to verify service failure**

Run:

```powershell
.\gradlew.bat test --tests com.dddblog.backend.member.application.RegisterMemberServiceTest
```

Expected: FAIL because `RegisterMemberService` does not check duplicate login ID or duplicate nickname yet.

- [ ] **Step 5: Update service duplicate checks**

Replace `backend/src/main/java/com/dddblog/backend/member/application/RegisterMemberService.java` with:

```java
package com.dddblog.backend.member.application;

import com.dddblog.backend.member.domain.LoginId;
import com.dddblog.backend.member.domain.Member;
import com.dddblog.backend.member.domain.MemberId;
import com.dddblog.backend.member.domain.MemberName;
import com.dddblog.backend.member.domain.Nickname;
import com.dddblog.backend.member.domain.PasswordHash;

public class RegisterMemberService {

	private final MemberRepository memberRepository;

	public RegisterMemberService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public MemberId register(RegisterMemberCommand command) {
		if (command == null) {
			throw new IllegalArgumentException("Register member command must not be null.");
		}
		MemberName name = new MemberName(command.name());
		Nickname nickname = new Nickname(command.nickname());
		LoginId loginId = new LoginId(command.loginId());
		PasswordHash passwordHash = new PasswordHash(command.passwordHash());
		if (memberRepository.existsByLoginId(loginId)) {
			throw new IllegalArgumentException("Login id already exists.");
		}
		if (memberRepository.existsByNickname(nickname)) {
			throw new IllegalArgumentException("Nickname already exists.");
		}
		MemberId memberId = memberRepository.nextId();
		Member member = Member.register(memberId, name, nickname, loginId, passwordHash);
		return memberRepository.save(member);
	}
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat test --tests com.dddblog.backend.member.application.RegisterMemberServiceTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run from repository root:

```powershell
cd C:\dev\study\ddd-blog
git add backend\src\test\java\com\dddblog\backend\member\application\RegisterMemberServiceTest.java backend\src\test\java\com\dddblog\backend\member\application\FakeMemberRepository.java backend\src\main\java\com\dddblog\backend\member\application\RegisterMemberService.java
git commit -m "feat: reject duplicate member registration values"
```

Expected: commit succeeds.

---

### Task 5: Full Verification

**Files:**
- Verify all files created or modified in Tasks 1-4.

- [ ] **Step 1: Run the full backend test suite**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 2: Check Korean test naming rule**

Run:

```powershell
Get-ChildItem -Path .\src\test\java\com\dddblog\backend -Recurse -Filter *.java | Select-String -Pattern 'void [a-zA-Z][a-zA-Z0-9_]*\('
```

Expected: no output.

- [ ] **Step 3: Check pure member domain/application packages**

Run:

```powershell
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\member\domain,.\src\main\java\com\dddblog\backend\member\application -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional'
```

Expected: no output.

- [ ] **Step 4: Confirm only planned files changed**

Run from repository root:

```powershell
cd C:\dev\study\ddd-blog
git status --short
```

Expected: no uncommitted files after the Task 4 commit.

---

## Self-Review

- Spec coverage: Tasks 1-4 add `RegisterMemberCommand`, `RegisterMemberService`, `MemberRepository`, `FakeMemberRepository`, and all required `RegisterMemberServiceTest` scenarios from the spec.
- Scope check: The plan adds no JPA entity, Spring Data repository, REST API, Spring bean configuration, BCrypt, login, JWT, member update, member withdrawal, or Post FK changes.
- Type consistency: `RegisterMemberCommand` has `name`, `nickname`, `loginId`, and `passwordHash`; `MemberRepository` has `existsByLoginId`, `existsByNickname`, `nextId`, and `save`; `RegisterMemberService.register(...)` returns `MemberId`.
- Ordering check: `RegisterMemberService` converts command values to value objects, checks duplicates, then calls `nextId()`, then `Member.register(...)`, then `save(...)`.
- Verification coverage: Task 5 includes full tests, Korean test name scan, and pure package annotation scan.
