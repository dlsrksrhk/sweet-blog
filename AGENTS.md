# AGENTS.md instructions for C:\dev\study\ddd-blog

## Project Rules

- 백엔드 테스트 메서드명은 IntelliJ 테스트 결과 목록에서 한국어 행동 설명으로 읽히도록 한글 시나리오형으로 작성한다.
- 한글 테스트 메서드명은 단어 사이를 `_`로 연결한다.
- 예: `본문이_비어_있으면_생성할_수_없다`
- 순수 도메인 테스트는 Spring Context를 띄우지 않는다.
- 도메인 클래스에는 Spring annotation을 붙이지 않는다.
- 도메인 규칙은 가능한 한 값 객체와 aggregate 안에 둔다.
- 이번 프로젝트는 DDD와 TDD 학습용이므로 작은 실패 테스트를 먼저 만들고 최소 구현으로 통과시킨다.
