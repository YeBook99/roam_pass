---
name: layered-architecture
description: Spring Boot 백엔드를 레이어드 아키텍처 베스트 프랙티스(ARCHITECTURE.md)대로 만든다. 백엔드 뼈대가 없을 때는 init(global 설정·예외 처리·BaseTimeEntity·ArchUnit 테스트 생성), 새 기능·API·도메인을 추가할 때는 feature(설계안 확인 → 같은 구조로 구현 → 체크리스트 점검). "프로젝트 뼈대 만들어", "백엔드 시작", "새 기능 개발", "새 API", "도메인 추가" 요청 시 사용.
---

# Skill: layered-architecture

## Purpose

모든 백엔드 기능이 같은 레이어드 구조를 갖게 한다. 설계 기준은 같은 폴더의 `ARCHITECTURE.md` 하나이고, 이 스킬은 그 문서를 **뼈대 생성(init)**과 **기능 추가(feature)** 시점에 적용하는 절차다. 코드 모양은 `templates/`를 따른다.

## 공통 준비 (두 모드 모두)

1. 이 폴더의 `ARCHITECTURE.md`, `RULES.md`, `templates/README.md`, 그리고 `.claude/skills/lombok-support/SKILL.md`를 읽는다.
2. 프로젝트 `CLAUDE.md`에서 스택(Java·Spring Boot 버전, DB, 마이그레이션 도구)과 불변 규칙을 확인한다. 불변 규칙이 `ARCHITECTURE.md`와 다르면 **프로젝트 규칙이 우선**이다(ARCHITECTURE.md §1). 다른 점은 설계안에 적는다.
3. 백엔드 루트를 정한다. 프로젝트 문서에 위치가 있으면 그곳, 없으면 `backend/`. 기본 패키지(`{base}`)도 프로젝트 문서에서 찾고, 없으면 사용자에게 묻는다.

## Procedure — init (뼈대 생성)

백엔드 루트에 `build.gradle`이 없거나 `global` 패키지가 없을 때 쓴다.

1. **프로젝트가 없으면 만든다.** Spring Initializr(`https://start.spring.io/starter.zip`)로 Gradle wrapper가 포함된 프로젝트를 받는다. 의존성: `web, data-jpa, validation, flyway, postgresql, lombok, testcontainers`.
   - Initializr가 프로젝트의 Spring Boot 버전을 더 이상 제공하지 않으면(예: 3.3.x 요청 시 400) 기본 버전으로 받은 뒤 다음처럼 맞춘다. 2026-10 기준 Boot 3.3.13 + Gradle 8.10.2 조합으로 확인했다.
     - `build.gradle`을 `templates/build/build.gradle.tmpl`로 바꾼다(최신 Boot의 starter 이름은 3.x와 다르다)
     - `gradle/wrapper/gradle-wrapper.properties`의 `distributionUrl`을 그 Boot 버전이 지원하는 Gradle(3.3.x는 8.x)로 바꾼다
     - 최신 Boot용으로 생성된 테스트 보조 파일(`TestcontainersConfiguration`, `Test{App}Application`)은 지우고 `IntegrationTestSupport`를 쓴다
2. `templates/build/build.gradle.tmpl`과 비교해 빠진 의존성(ArchUnit, Testcontainers 등)을 추가한다. **새 라이브러리를 추가했으면 `/adr`로 근거를 남긴다.**
3. `templates/build/application.yml.tmpl`을 반영한다. 접속 정보는 `${ENV}` 자리표시자만 쓴다.
4. `templates/init/` 아래 파일을 `templates/README.md`의 위치 표대로 만든다: `global.config`(Clock, JPA Auditing), `global.error`(ErrorCode, CommonErrorCode, BusinessException, GlobalExceptionHandler), `global.entity.BaseTimeEntity`, `ArchitectureTest`, `IntegrationTestSupport`.
5. `./gradlew test`를 실행한다. 컨텍스트 로딩 테스트(`*ApplicationTests`)는 DB가 필요하므로 `IntegrationTestSupport`를 상속하게 바꾸거나, Docker가 없으면 실행하지 못한 사실을 보고한다.
6. 만든 파일 표(파일 / 계층 / 한 줄 책임)와 테스트 결과를 보고한다.

## Procedure — feature `<기능 이름>` (기능 추가)

1. 공통 준비를 한다. init이 안 되어 있으면 init부터 하자고 제안한다.
2. 기존 코드에서 같은 기능 패키지, 재사용할 `ErrorCode`·`infra` Client, 비슷한 Service가 있는지 Glob/Grep으로 찾는다.
3. **코드를 쓰기 전에** 아래 설계안을 보여 주고 사용자 확인을 받는다.

   ```
   ## 기능 설계안 — <기능 이름> (<HTTP 메서드> <경로>, …)

   ### 파일 구성
   {base}.<feature>
   ├─ controller/XxxController.java    <한 줄 책임>
   ├─ service/XxxService.java          인터페이스: 밖에 내놓는 일 <한 줄 책임>
   ├─ service/impl/XxxServiceImpl.java 구현: 트랜잭션·업무 코드
   ├─ repository/...
   ├─ domain/...
   ├─ dto/...
   └─ exception/XxxErrorCode.java
   (외부 연동이 있으면 {base}.infra.<system>/… 도)
   db/migration/V<n>__<설명>.sql

   | 결정 | 내용 | 근거 |
   |---|---|---|
   | 트랜잭션 | 조회 readOnly / 쓰기 @Transactional / 외부 호출 → TransactionTemplate tx1→호출→tx2 | §6, §7 |
   | 외부 호출 실패 시 | 없음 / tx1 기록을 누가 마무리하나(재조회·스케줄러·운영자), 재시도 상한 | §7-5·6 |
   | 다른 기능 의존 | 없음 / 어느 기능의 어떤 Service, 이벤트 여부 | §4-5·6, §6.4 |
   | 에러 코드 | XXX_NOT_FOUND(404) … | §10 |
   | 중복·동시성 | 없음 / UNIQUE 제약, 조건부 UPDATE | §11 |
   | 테스트 | 테스트 클래스와 검증할 관찰 결과 | §16 |

   ### 프로젝트 규칙 때문에 문서와 다르게 하는 점
   없음 / <무엇을, 어떤 규칙 때문에>
   ```
4. 확인을 받으면 `templates/feature/`(외부 연동이 처음이면 `templates/infra/`)를 바탕으로 **domain → repository → dto·exception → service → controller → 마이그레이션 → 테스트** 순서로 만든다. 예시 필드는 기능에 맞게 바꾼다.
5. `./gradlew test`를 실행한다. `ArchitectureTest`가 실패하면 테스트가 아니라 코드를 고친다.
6. `ARCHITECTURE.md` §17 체크리스트를 항목마다 확인해 표(항목 / ✅·❌ / 근거 `파일:줄`)로 보고한다. ❌는 고치거나 이유를 적는다.
7. 다음 작업으로 넘긴다.
   - 상태를 추가·변경했다 → `/state-transition`
   - 새 라이브러리를 넣었거나 문서와 다르게 결정했다 → `/adr`
   - 커밋 → `/commit`

## Heuristics

- 설계안의 결정마다 근거 절(`ARCHITECTURE.md §n`)을 붙인다. 근거를 댈 수 없는 결정은 다시 검토한다.
- 조회만 하는 기능도 Controller → Service → Repository를 거친다. 작다는 이유로 계층을 생략하지 않는다.
- 외부 HTTP 호출이 하나라도 있으면 그 Service 메서드는 `@Transactional` 없이 `TransactionTemplate`으로 나눈다. Facade 같은 계층을 새로 만들지 않는다.
- 두 기능이 함께 쓰는 업무 코드는 global이 아니라 주인 기능에 둔다. 주인이 없으면 새 기능 패키지를 제안한다.
- 다른 기능의 Entity는 ID로 참조한다.
- 설계안의 파일이 15개를 넘으면 기능을 나눌 수 있는지 먼저 제안한다.

## Do Not

- 설계안을 보여 주지 않고 바로 구현하지 않는다.
- `ARCHITECTURE.md`에 없는 계층이나 패키지(`facade`, `manager`, `util`, `helper`, `common`, `api`)를 만들지 않는다.
- 문서와 기존 코드가 다를 때 말없이 한쪽을 따르지 않는다. 차이를 보고하고 어느 쪽으로 맞출지 묻는다.
- `ArchitectureTest`를 통과시키려고 규칙을 지우거나 약하게 바꾸지 않는다. 규칙을 바꿔야 하면 `ARCHITECTURE.md`부터 고치고 사용자 확인을 받는다.
- 실행하지 않은 테스트나 확인하지 않은 체크리스트 항목을 통과로 보고하지 않는다.
