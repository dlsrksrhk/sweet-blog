# Member Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first pure Java `Member` domain model with value objects, role/status enums, and Korean scenario-style domain tests.

**Architecture:** Keep the new member model in `com.dddblog.backend.member.domain`, separate from the existing `blog` context. This slice adds no Spring, JPA, API, repository, password encoder, login, or duplicate-checking behavior. Each domain rule is introduced by a small failing unit test, then the minimal implementation.

**Tech Stack:** Java 21, JUnit 5, AssertJ, Gradle, Spring Boot test dependencies already present.

---

## File Structure

- Create: `backend/src/main/java/com/dddblog/backend/member/domain/MemberId.java`
  - Long-backed member identifier value object.
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/MemberName.java`
  - Trimmed real-name value object.
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/Nickname.java`
  - Trimmed display nickname value object.
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/LoginId.java`
  - Trimmed login identifier value object.
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/PasswordHash.java`
  - Hashed password value object that stores the given hash unchanged.
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/MemberRole.java`
  - Member role enum.
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/MemberStatus.java`
  - Member account status enum.
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/Member.java`
  - Pure member aggregate with required fields and `register` factory.
- Create test files under `backend/src/test/java/com/dddblog/backend/member/domain`.

Do not modify existing `blog` domain, application, or persistence classes in this plan.

---

### Task 1: `MemberId` Value Object

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/member/domain/MemberIdTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/MemberId.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/dddblog/backend/member/domain/MemberIdTest.java`:

```java
package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MemberIdTest {

	@Test
	void 회원_ID를_생성한다() {
		MemberId memberId = new MemberId(1L);

		assertThat(memberId.value()).isEqualTo(1L);
	}

	@Test
	void 회원_ID가_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new MemberId(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member id must not be null.");
	}

	@Test
	void 회원_ID가_0이면_생성할_수_없다() {
		assertThatThrownBy(() -> new MemberId(0L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member id must be positive.");
	}

	@Test
	void 회원_ID가_음수이면_생성할_수_없다() {
		assertThatThrownBy(() -> new MemberId(-1L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member id must be positive.");
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd C:\dev\study\ddd-blog\backend
$env:JAVA_HOME='C:\java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.dddblog.backend.member.domain.MemberIdTest
```

Expected: FAIL because `MemberId` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `backend/src/main/java/com/dddblog/backend/member/domain/MemberId.java`:

```java
package com.dddblog.backend.member.domain;

import java.util.Objects;

public final class MemberId {

	private final Long value;

	public MemberId(Long value) {
		if (value == null) {
			throw new IllegalArgumentException("Member id must not be null.");
		}
		if (value < 1) {
			throw new IllegalArgumentException("Member id must be positive.");
		}
		this.value = value;
	}

	public Long value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof MemberId memberId)) {
			return false;
		}
		return Objects.equals(value, memberId.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
.\gradlew.bat test --tests com.dddblog.backend.member.domain.MemberIdTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add backend\src\test\java\com\dddblog\backend\member\domain\MemberIdTest.java backend\src\main\java\com\dddblog\backend\member\domain\MemberId.java
git commit -m "feat: add member id value object"
```

---

### Task 2: Member Profile Value Objects

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/member/domain/MemberNameTest.java`
- Create: `backend/src/test/java/com/dddblog/backend/member/domain/NicknameTest.java`
- Create: `backend/src/test/java/com/dddblog/backend/member/domain/LoginIdTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/MemberName.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/Nickname.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/LoginId.java`

- [ ] **Step 1: Write failing tests**

Create `backend/src/test/java/com/dddblog/backend/member/domain/MemberNameTest.java`:

```java
package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MemberNameTest {

	@Test
	void 회원_이름을_생성한다() {
		MemberName name = new MemberName("홍길동");

		assertThat(name.value()).isEqualTo("홍길동");
	}

	@Test
	void 이름은_앞뒤_공백을_제거한다() {
		MemberName name = new MemberName(" 홍길동 ");

		assertThat(name.value()).isEqualTo("홍길동");
	}

	@Test
	void 이름이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new MemberName(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member name must not be blank.");
	}

	@Test
	void 이름이_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new MemberName(" "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member name must not be blank.");
	}

	@Test
	void 이름이_30자를_초과하면_생성할_수_없다() {
		assertThatThrownBy(() -> new MemberName("가".repeat(31)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member name must be 30 characters or less.");
	}
}
```

Create `backend/src/test/java/com/dddblog/backend/member/domain/NicknameTest.java`:

```java
package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NicknameTest {

	@Test
	void 닉네임을_생성한다() {
		Nickname nickname = new Nickname("길동");

		assertThat(nickname.value()).isEqualTo("길동");
	}

	@Test
	void 닉네임은_앞뒤_공백을_제거한다() {
		Nickname nickname = new Nickname(" 길동 ");

		assertThat(nickname.value()).isEqualTo("길동");
	}

	@Test
	void 닉네임이_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new Nickname(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname must not be blank.");
	}

	@Test
	void 닉네임이_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new Nickname(" "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname must not be blank.");
	}

	@Test
	void 닉네임이_2자보다_짧으면_생성할_수_없다() {
		assertThatThrownBy(() -> new Nickname("가"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname must be 2 characters or more.");
	}

	@Test
	void 닉네임이_20자를_초과하면_생성할_수_없다() {
		assertThatThrownBy(() -> new Nickname("가".repeat(21)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Nickname must be 20 characters or less.");
	}
}
```

Create `backend/src/test/java/com/dddblog/backend/member/domain/LoginIdTest.java`:

```java
package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LoginIdTest {

	@Test
	void 로그인_ID를_생성한다() {
		LoginId loginId = new LoginId("user01");

		assertThat(loginId.value()).isEqualTo("user01");
	}

	@Test
	void 로그인_ID는_앞뒤_공백을_제거한다() {
		LoginId loginId = new LoginId(" user01 ");

		assertThat(loginId.value()).isEqualTo("user01");
	}

	@Test
	void 로그인_ID가_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new LoginId(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id must not be blank.");
	}

	@Test
	void 로그인_ID가_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new LoginId(" "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id must not be blank.");
	}

	@Test
	void 로그인_ID가_4자보다_짧으면_생성할_수_없다() {
		assertThatThrownBy(() -> new LoginId("abc"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id must be 4 characters or more.");
	}

	@Test
	void 로그인_ID가_30자를_초과하면_생성할_수_없다() {
		assertThatThrownBy(() -> new LoginId("a".repeat(31)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Login id must be 30 characters or less.");
	}
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat test --tests "com.dddblog.backend.member.domain.*Test"
```

Expected: FAIL because `MemberName`, `Nickname`, and `LoginId` do not exist.

- [ ] **Step 3: Write minimal implementations**

Create `backend/src/main/java/com/dddblog/backend/member/domain/MemberName.java`:

```java
package com.dddblog.backend.member.domain;

import java.util.Objects;

public final class MemberName {

	private static final int MAX_LENGTH = 30;

	private final String value;

	public MemberName(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Member name must not be blank.");
		}
		String trimmedValue = value.trim();
		if (trimmedValue.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Member name must be 30 characters or less.");
		}
		this.value = trimmedValue;
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof MemberName memberName)) {
			return false;
		}
		return Objects.equals(value, memberName.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return value;
	}
}
```

Create `backend/src/main/java/com/dddblog/backend/member/domain/Nickname.java`:

```java
package com.dddblog.backend.member.domain;

import java.util.Objects;

public final class Nickname {

	private static final int MIN_LENGTH = 2;
	private static final int MAX_LENGTH = 20;

	private final String value;

	public Nickname(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Nickname must not be blank.");
		}
		String trimmedValue = value.trim();
		if (trimmedValue.length() < MIN_LENGTH) {
			throw new IllegalArgumentException("Nickname must be 2 characters or more.");
		}
		if (trimmedValue.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Nickname must be 20 characters or less.");
		}
		this.value = trimmedValue;
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof Nickname nickname)) {
			return false;
		}
		return Objects.equals(value, nickname.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return value;
	}
}
```

Create `backend/src/main/java/com/dddblog/backend/member/domain/LoginId.java`:

```java
package com.dddblog.backend.member.domain;

import java.util.Objects;

public final class LoginId {

	private static final int MIN_LENGTH = 4;
	private static final int MAX_LENGTH = 30;

	private final String value;

	public LoginId(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Login id must not be blank.");
		}
		String trimmedValue = value.trim();
		if (trimmedValue.length() < MIN_LENGTH) {
			throw new IllegalArgumentException("Login id must be 4 characters or more.");
		}
		if (trimmedValue.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Login id must be 30 characters or less.");
		}
		this.value = trimmedValue;
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof LoginId loginId)) {
			return false;
		}
		return Objects.equals(value, loginId.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return value;
	}
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat test --tests "com.dddblog.backend.member.domain.*Test"
```

Expected: PASS for `MemberIdTest`, `MemberNameTest`, `NicknameTest`, and `LoginIdTest`.

- [ ] **Step 5: Commit**

```powershell
git add backend\src\test\java\com\dddblog\backend\member\domain\MemberNameTest.java backend\src\test\java\com\dddblog\backend\member\domain\NicknameTest.java backend\src\test\java\com\dddblog\backend\member\domain\LoginIdTest.java backend\src\main\java\com\dddblog\backend\member\domain\MemberName.java backend\src\main\java\com\dddblog\backend\member\domain\Nickname.java backend\src\main\java\com\dddblog\backend\member\domain\LoginId.java
git commit -m "feat: add member profile value objects"
```

---

### Task 3: Password Hash And Member Enums

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/member/domain/PasswordHashTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/PasswordHash.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/MemberRole.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/MemberStatus.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/dddblog/backend/member/domain/PasswordHashTest.java`:

```java
package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PasswordHashTest {

	@Test
	void 비밀번호_해시를_생성한다() {
		PasswordHash passwordHash = new PasswordHash("$2a$10$hashed-password");

		assertThat(passwordHash.value()).isEqualTo("$2a$10$hashed-password");
	}

	@Test
	void 비밀번호_해시가_null이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PasswordHash(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password hash must not be blank.");
	}

	@Test
	void 비밀번호_해시가_blank이면_생성할_수_없다() {
		assertThatThrownBy(() -> new PasswordHash(" "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Password hash must not be blank.");
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat test --tests com.dddblog.backend.member.domain.PasswordHashTest
```

Expected: FAIL because `PasswordHash` does not exist.

- [ ] **Step 3: Write minimal implementations**

Create `backend/src/main/java/com/dddblog/backend/member/domain/PasswordHash.java`:

```java
package com.dddblog.backend.member.domain;

import java.util.Objects;

public final class PasswordHash {

	private final String value;

	public PasswordHash(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Password hash must not be blank.");
		}
		this.value = value;
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof PasswordHash passwordHash)) {
			return false;
		}
		return Objects.equals(value, passwordHash.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return value;
	}
}
```

Create `backend/src/main/java/com/dddblog/backend/member/domain/MemberRole.java`:

```java
package com.dddblog.backend.member.domain;

public enum MemberRole {
	MEMBER,
	ADMIN
}
```

Create `backend/src/main/java/com/dddblog/backend/member/domain/MemberStatus.java`:

```java
package com.dddblog.backend.member.domain;

public enum MemberStatus {
	ACTIVE,
	INACTIVE
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
.\gradlew.bat test --tests com.dddblog.backend.member.domain.PasswordHashTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add backend\src\test\java\com\dddblog\backend\member\domain\PasswordHashTest.java backend\src\main\java\com\dddblog\backend\member\domain\PasswordHash.java backend\src\main\java\com\dddblog\backend\member\domain\MemberRole.java backend\src\main\java\com\dddblog\backend\member\domain\MemberStatus.java
git commit -m "feat: add member password hash and enums"
```

---

### Task 4: `Member` Aggregate

**Files:**
- Create: `backend/src/test/java/com/dddblog/backend/member/domain/MemberTest.java`
- Create: `backend/src/main/java/com/dddblog/backend/member/domain/Member.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/dddblog/backend/member/domain/MemberTest.java`:

```java
package com.dddblog.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MemberTest {

	@Test
	void 신규_회원을_등록한다() {
		Member member = registerMember();

		assertThat(member.id()).isEqualTo(new MemberId(1L));
		assertThat(member.name()).isEqualTo(new MemberName("홍길동"));
		assertThat(member.nickname()).isEqualTo(new Nickname("길동"));
		assertThat(member.loginId()).isEqualTo(new LoginId("user01"));
		assertThat(member.passwordHash()).isEqualTo(new PasswordHash("$2a$10$hashed-password"));
	}

	@Test
	void 신규_회원은_MEMBER_권한을_가진다() {
		Member member = registerMember();

		assertThat(member.role()).isEqualTo(MemberRole.MEMBER);
	}

	@Test
	void 신규_회원은_ACTIVE_상태를_가진다() {
		Member member = registerMember();

		assertThat(member.status()).isEqualTo(MemberStatus.ACTIVE);
	}

	@Test
	void ID가_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			null,
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("$2a$10$hashed-password"),
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member id must not be null.");
	}

	@Test
	void 이름이_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			new MemberId(1L),
			null,
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("$2a$10$hashed-password"),
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member name must not be null.");
	}

	@Test
	void 닉네임이_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			null,
			new LoginId("user01"),
			new PasswordHash("$2a$10$hashed-password"),
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member nickname must not be null.");
	}

	@Test
	void 로그인_ID가_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			null,
			new PasswordHash("$2a$10$hashed-password"),
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member login id must not be null.");
	}

	@Test
	void 비밀번호_해시가_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			null,
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member password hash must not be null.");
	}

	@Test
	void 권한이_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("$2a$10$hashed-password"),
			null,
			MemberStatus.ACTIVE
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member role must not be null.");
	}

	@Test
	void 상태가_없으면_회원을_생성할_수_없다() {
		assertThatThrownBy(() -> new Member(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("$2a$10$hashed-password"),
			MemberRole.MEMBER,
			null
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Member status must not be null.");
	}

	private Member registerMember() {
		return Member.register(
			new MemberId(1L),
			new MemberName("홍길동"),
			new Nickname("길동"),
			new LoginId("user01"),
			new PasswordHash("$2a$10$hashed-password")
		);
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat test --tests com.dddblog.backend.member.domain.MemberTest
```

Expected: FAIL because `Member` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `backend/src/main/java/com/dddblog/backend/member/domain/Member.java`:

```java
package com.dddblog.backend.member.domain;

public final class Member {

	private final MemberId id;
	private final MemberName name;
	private final Nickname nickname;
	private final LoginId loginId;
	private final PasswordHash passwordHash;
	private final MemberRole role;
	private final MemberStatus status;

	public Member(
		MemberId id,
		MemberName name,
		Nickname nickname,
		LoginId loginId,
		PasswordHash passwordHash,
		MemberRole role,
		MemberStatus status
	) {
		if (id == null) {
			throw new IllegalArgumentException("Member id must not be null.");
		}
		if (name == null) {
			throw new IllegalArgumentException("Member name must not be null.");
		}
		if (nickname == null) {
			throw new IllegalArgumentException("Member nickname must not be null.");
		}
		if (loginId == null) {
			throw new IllegalArgumentException("Member login id must not be null.");
		}
		if (passwordHash == null) {
			throw new IllegalArgumentException("Member password hash must not be null.");
		}
		if (role == null) {
			throw new IllegalArgumentException("Member role must not be null.");
		}
		if (status == null) {
			throw new IllegalArgumentException("Member status must not be null.");
		}
		this.id = id;
		this.name = name;
		this.nickname = nickname;
		this.loginId = loginId;
		this.passwordHash = passwordHash;
		this.role = role;
		this.status = status;
	}

	public static Member register(
		MemberId id,
		MemberName name,
		Nickname nickname,
		LoginId loginId,
		PasswordHash passwordHash
	) {
		return new Member(
			id,
			name,
			nickname,
			loginId,
			passwordHash,
			MemberRole.MEMBER,
			MemberStatus.ACTIVE
		);
	}

	public MemberId id() {
		return id;
	}

	public MemberName name() {
		return name;
	}

	public Nickname nickname() {
		return nickname;
	}

	public LoginId loginId() {
		return loginId;
	}

	public PasswordHash passwordHash() {
		return passwordHash;
	}

	public MemberRole role() {
		return role;
	}

	public MemberStatus status() {
		return status;
	}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
.\gradlew.bat test --tests com.dddblog.backend.member.domain.MemberTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add backend\src\test\java\com\dddblog\backend\member\domain\MemberTest.java backend\src\main\java\com\dddblog\backend\member\domain\Member.java
git commit -m "feat: add member aggregate"
```

---

### Task 5: Full Verification

**Files:**
- Verify all files created in Tasks 1-4.

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

- [ ] **Step 3: Check member domain purity**

Run:

```powershell
Get-ChildItem -Path .\src\main\java\com\dddblog\backend\member\domain -Recurse -Filter *.java | Select-String -Pattern '@Component|@Service|@Repository|@Entity|@Embeddable|@Table|@Transactional'
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

- Spec coverage: Tasks 1-4 cover `MemberId`, `MemberName`, `Nickname`, `LoginId`, `PasswordHash`, `MemberRole`, `MemberStatus`, `Member`, and all required tests from the spec.
- Scope check: No repository, JPA, Spring annotation, REST API, BCrypt, login, JWT, duplicate-checking, member update, withdrawal, or Post FK work is included.
- Type consistency: The plan consistently uses `Member.register(MemberId, MemberName, Nickname, LoginId, PasswordHash)`, `value()` accessors for value objects, and aggregate accessors `id()`, `name()`, `nickname()`, `loginId()`, `passwordHash()`, `role()`, `status()`.
- Placeholder scan: No incomplete placeholder markers remain.
