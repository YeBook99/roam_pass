# 02. 기술 선택 기록 (ADR)

> **ADR(Architecture Decision Record)**: "무엇을, 왜 골랐고, 무엇을 버렸는지"를 결정할 때마다 한 장씩 남기는 기록.
> 과제 2.2는 "무엇을 골랐는지가 아니라 **선택에 이유가 있는지**"를 본다고 했다. "익숙해서"도 좋은 이유라고 했으므로 솔직하게 쓴다.
>
> 각 ADR의 **"처음 써 봄?"** 칸은 질문 7(처음 써 본 것)의 재료다. ✍️ 표시는 **본인이 직접 채워야 하는 칸**이다(대신 지어내면 인터뷰에서 들통난다).

## 요약표

| # | 영역 | 결정 | 한 줄 이유 |
|---|---|---|---|
| 01 | 프론트 라우터 | Next.js 15 **App Router** | 공식 기본값, 서버/클라이언트 경계를 파일 단위로 명확히 나눌 수 있음 |
| 02 | 데이터 패칭 | Server Component fetch + **TanStack Query** 폴링 | 초기 표시는 서버에서, 발급 대기는 폴링 중단 조건을 선언적으로 |
| 03 | 백엔드 언어 | **Java 17 + Spring Boot 3.5 + Gradle** | 익숙함, Boot 3.x 최소 요구가 Java 17 |
| 04 | ORM/쿼리 | **Spring Data JPA + 네이티브 조건부 쿼리** | CRUD는 빠르게, 동시성 핵심은 SQL로 눈에 보이게 |
| 05 | 비동기 | **AFTER_COMMIT 이벤트 + @Async + @Scheduled** | 브로커 없이 "즉시 처리 + DB 기반 안전망" |
| 06 | DB | **PostgreSQL 16** | 회사 사용 DB, `ON CONFLICT`·`timestamptz`·부분 인덱스 |
| 07 | 마이그레이션 | **Flyway** | 빈 DB에서 스키마+시드 재현이 제출 요건 |
| 08 | 아키텍처 스타일 | **도메인별 패키지 + 3계층** | 설명 가능한 범위 유지 |
| 09 | 모의 PG·공급사 위치 | **같은 앱, 별도 패키지, 실제 HTTP 호출** | 배포 1개 + 네트워크 문제 재현 |
| 10 | HTTP 클라이언트 | **Spring RestClient** | 동기 코드, 타임아웃 설정 간단 |
| 11 | 테스트 | **JUnit5 + Testcontainers + Awaitility** | H2로는 동시성·`ON CONFLICT` 재현 불가 |
| 12 | 배포 | **AWS 프리티어 EC2 + RDS** | 서버가 잠들지 않아 스케줄러 유지 |
| 13 | 서명 | **HMAC-SHA256 (JDK `javax.crypto`)** | 과제 명세, 외부 라이브러리 불필요 |
| 14 | PR 화면 캡처 | **Playwright MCP** | 같은 조건으로 before/after 캡처. 필수 테스트는 백엔드에서 증명 |

---

### ADR-01. Next.js App Router 사용

- **맥락**: 2.2와 Q1·Q6은 "어떤 라우터를 왜 골랐는지", "서버/클라이언트 경계를 어디에 그었는지"를 묻는다. 나는 **Next.js가 처음**이다
- **결정**: Next.js 15의 **App Router**(`app/` 폴더)를 쓴다
- **고려한 대안**

  | 대안 | 장점 | 버린 이유 |
  |---|---|---|
  | Pages Router (`pages/`) | 자료가 많고 구조가 단순함 | 현재 공식 문서와 신규 예제가 App Router 기준. 처음 배우는 입장에서 지금 표준을 익히는 게 낫다고 판단 |
  | Next 없이 React(Vite) SPA | 개념이 적음 | 과제가 Next.js를 지정함 |

- **근거 (1년차 말투)**: "Next.js를 처음 써서 공식 튜토리얼(nextjs.org/learn)을 따라갔는데, 튜토리얼이 App Router 기준이었습니다. App Router는 컴포넌트마다 서버에서 돌지 브라우저에서 돌지를 `'use client'` 한 줄로 정하게 돼 있어서, 과제가 묻는 '서버/클라이언트 경계'를 파일 단위로 보여 주기 좋다고 생각했습니다."
- **처음 써 봄?**: 예. 학습 정리는 [L1 Next.js 기초](learn/L1_Nextjs_기초.md)
  > ✍️ 직접 작성: 무엇으로 배웠는지(공식 튜토리얼/영상/AI), 걸린 시간, 헷갈렸던 점(예: Server Component에서 useState를 써서 빌드 에러)
- **한계와 바꿀 조건**: App Router의 캐시 동작이 버전마다 바뀌어 헷갈린다. 결제 상태처럼 최신이어야 하는 데이터는 전부 `cache: 'no-store'`로 캐시를 끄고 단순하게 갔다

### ADR-02. 데이터 패칭: Server Component 초기 조회 + TanStack Query 폴링

- **맥락**: 발급은 몇 초 뒤 콜백으로 완료된다. 대기 화면은 결과를 "어떻게 받는가"를 설명해야 한다(Q6)
- **결정**
  - 페이지 진입 시 데이터는 **Server Component**에서 `fetch(..., { cache: 'no-store' })`로 가져와 완성된 HTML로 보낸다
  - 바뀌는 데이터(발급 대기, 어드민 자동 새로고침)는 **Client Component**에서 TanStack Query `useQuery({ refetchInterval })`로 폴링한다
  - 쓰기(결제 요청, 환불 버튼)는 TanStack Query `useMutation`으로 처리하고, `Idempotency-Key` 헤더를 직접 붙인다
- **고려한 대안**

  | 대안 | 장점 | 버린 이유 |
  |---|---|---|
  | `useEffect` + `setInterval` 직접 작성 | 라이브러리 없음 | 언마운트 정리, 중복 요청, 에러 시 재시도, "완료되면 중단"을 다 직접 짜야 함 |
  | SWR | 가볍고 비슷함 | 둘 다 처음이라 차이가 크지 않음. TanStack Query는 `refetchInterval`에 함수를 넣어 "최종 상태면 false" 처리가 직관적이고 mutation 지원이 명확해서 선택 |
  | SSE(Server-Sent Events) | 서버가 즉시 푸시, 요청 수 적음 | 연결을 유지하는 동안 서버 스레드/커넥션 관리 필요, 프록시 타임아웃 고려 필요. 발급은 보통 수 초면 끝나서 2초 폴링으로 충분 |
  | WebSocket | 양방향 | 단방향 알림에 과함 |
  | Server Actions | 폼 제출을 서버 함수로 간단히 | 처음 배우는 개념이 하나 더 늘어남. 멱등키 헤더를 명시적으로 다루기 위해 일반 API 호출 방식 선택 |

- **근거**: "결제 결과는 몇 초 늦을 수 있어서 화면이 스스로 다시 물어봐야 했습니다. 직접 setInterval로 짜 보니 화면을 떠나도 요청이 계속 나가는 문제가 생길 수 있다는 걸 알게 됐고, TanStack Query는 화면을 떠나면 자동으로 멈추고 '발급 완료면 그만 물어봐'를 한 줄로 쓸 수 있어서 골랐습니다."
- **처음 써 봄?**: ✍️
- **한계**: 사용자 1만 명이 동시에 대기 화면에 있으면 2초마다 1만 요청이 온다. 그 규모라면 SSE나 지수적으로 늘어나는 폴링 간격을 고려한다

### ADR-03. Java 17 + Spring Boot 3.5 + Gradle

- **결정**: Java 17, Spring Boot 3.5.16, Gradle 8.14.6(Groovy DSL, wrapper 포함)
- **버전을 바꾼 이유**: 처음에는 3.3으로 정했는데, 3.3은 오픈소스 지원이 끝난 버전이었다. 4.x는 Spring Framework 7·Jackson 3 등 바뀐 것이 많아 3.x 자료가 맞지 않는 곳이 생기므로, 3.x의 마지막 라인인 3.5를 골랐다
- **만든 방법**: Spring Initializr(start.spring.io)가 이제 4.x 프로젝트만 만들어 줘서, Gradle wrapper 파일만 받고 `build.gradle`은 3.5 기준으로 직접 썼다. wrapper jar는 Gradle 공식 체크섬과 같은지 확인했다
- **함께 넣은 starter**: `spring-boot-starter-actuator`. `/actuator/health`를 docker compose healthcheck(`up --wait`)와 배포 후 헬스 체크에 쓴다. 노출하는 엔드포인트는 `health` 하나뿐이다
- **대안**: Kotlin(간결하지만 처음 배우는 것이 하나 더 늘어남), Maven(익숙도 차이 외 큰 차이 없음), Java 21(가상 스레드가 있지만 로컬에 17만 설치되어 있고 이 과제에 꼭 필요하지 않음)
- **근거**: "Java와 Spring은 써 본 적이 있어서 익숙한 쪽을 골랐습니다. Next.js가 처음이라 백엔드까지 새로운 걸 쓰면 둘 다 설명을 못 할 것 같았습니다. Boot 3.x는 Java 17 이상이 필수라서 17로 맞췄습니다."
- **처음 써 봄?**: ✍️ (Spring Boot 3에서 처음 접한 것, 예: `RestClient`, `jakarta.*` 패키지)

### ADR-04. Spring Data JPA + 네이티브 조건부 쿼리

- **맥락**: 2.2와 Q1은 "ORM과 쿼리 방식"을 묻고, Q3은 "중복을 어떻게 막았는지"를 묻는다
- **결정**
  - 일반적인 저장과 조회는 **Spring Data JPA** 리포지토리로 처리한다
  - **상태를 바꾸는 쿼리는 전부** `@Modifying @Query(nativeQuery = true)`로 직접 쓴 **조건부 UPDATE**다. 반환값(영향 받은 행 수)으로 "내가 처리권을 가졌는지"를 판단한다(→ [L3 상태 선점](learn/L3_동시성_핵심개념.md#3-상태-선점))
  - 중복 이벤트 기록은 `INSERT ... ON CONFLICT (event_key) DO NOTHING`으로 한다
  - 어드민 목록 필터는 조건이 많아서 `JpaSpecificationExecutor`(동적 조건)로 만든다
- **고려한 대안**

  | 대안 | 장점 | 버린 이유 |
  |---|---|---|
  | JPA만 사용(엔티티 조회 → setter → 저장) | 코드가 객체지향적 | "조회 → 판단 → 저장" 사이에 다른 요청이 끼어들 수 있음(경쟁 상태). 막으려면 락이 필요 |
  | MyBatis | SQL을 전부 직접 제어 | 단순 CRUD까지 XML·매퍼를 써야 해서 코드가 많음 |
  | JPA + JdbcTemplate 혼용 | 조건부 쿼리를 JdbcTemplate으로 | JPA 네이티브 쿼리로도 같은 일이 되므로 방식 두 개를 섞을 이유가 없음 |
  | QueryDSL | 타입 안전 동적 쿼리 | 설정(코드 생성)이 Boot 3에서 번거롭고 처음 쓰는 도구가 하나 더 늘어남 |

- **근거**: "처음엔 JPA로 주문을 조회해서 상태가 PENDING이면 PAID로 바꾸고 저장하려 했습니다. 그런데 웹훅 두 개가 동시에 오면 둘 다 PENDING을 읽고 둘 다 처리할 수 있다는 걸 알았습니다. 그래서 '상태가 PENDING일 때만 바꿔라'를 UPDATE 한 문장에 넣어 DB에 맡겼습니다. JPA에서도 네이티브 쿼리로 쓸 수 있어서 다른 도구를 추가하지 않았습니다."
- **주의할 점(공부한 것)**
  - `@Modifying` 쿼리는 **영속성 컨텍스트**(JPA가 엔티티를 기억해 두는 1차 캐시)를 거치지 않는다. 같은 트랜잭션에서 그 엔티티를 다시 읽으면 옛날 값이 보일 수 있으므로 `@Modifying(clearAutomatically = true)`를 쓰거나, 업데이트 후에 다시 조회한다
  - `@Modifying` 쿼리는 트랜잭션 안에서 실행해야 한다
- **처음 써 봄?**: ✍️ (`ON CONFLICT`, `@Modifying` 사용 경험 여부)

### ADR-05. 비동기 처리: AFTER_COMMIT 이벤트 + @Async + @Scheduled

- **맥락**: 콜백 처리, 재시도, 상태 조회를 어떤 방식으로 하는지(2.2, Q1). 그리고 "브라우저를 닫아도 발급이 끝까지 진행돼야 한다"(2.3-3)
- **결정** (상세: [L2 Spring 비동기·스케줄러](learn/L2_Spring_비동기_스케줄러.md))
  1. **즉시 처리**: 결제가 확정되어 커밋되면 `OrderPaidEvent`를 발행한다. `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`가 이를 받아 별도 스레드에서 발급을 요청한다
  2. **안전망**: `@Scheduled`가 주기적으로 DB를 보고 "멈춰 있는 주문"(PAID 방치, 콜백 타임아웃, 환불 대기)을 다시 처리한다
  3. **콜백 처리**는 일반 컨트롤러가 동기로 처리한다. 서명 검증 → 중복 체크 → 상태 반영 후 200을 응답한다
- **고려한 대안**

  | 대안 | 장점 | 버린 이유 |
  |---|---|---|
  | 메시지 큐(Kafka, RabbitMQ, SQS) | 재시도·순서·확장성이 좋음 | 무료 배포에서 브로커를 하나 더 띄워야 하고 학습 비용이 큼. 이 과제 트래픽에는 과함 |
  | 스케줄러만 사용 | 단순 | 결제 후 발급 시작까지 최대 스케줄 주기(30초)만큼 지연됨 |
  | @Async만 사용 | 즉시 | 서버가 그 순간 죽으면 작업이 사라짐(메모리에만 있으니까) |
  | Spring Batch | 대량 처리 | 건별 실시간 처리에 맞지 않음 |
- **근거**: "@Async만 쓰면 빠르지만 서버가 꺼지면 작업이 날아가고, 스케줄러만 쓰면 안전하지만 느립니다. 그래서 평소에는 @Async로 바로 처리하고, 놓친 건 스케줄러가 DB 상태를 보고 주워 가게 했습니다. **진짜 작업 목록은 메모리가 아니라 DB 상태 자체**라서 서버가 재시작돼도 이어서 처리됩니다."
- **처음 써 봄?**: ✍️ (`@TransactionalEventListener`는 처음일 가능성이 높음)
- **한계**: 서버 2대 이상이면 스케줄러가 같은 주문을 동시에 집을 수 있다. 조건부 UPDATE 덕분에 결과는 한 번만 반영되지만 HTTP 호출은 중복될 수 있다. 해결책은 ShedLock(스케줄러 실행 락)이나 `SELECT ... FOR UPDATE SKIP LOCKED`다

### ADR-06. PostgreSQL 16

- **결정**: PostgreSQL. 로컬은 docker compose, 배포는 AWS RDS
- **대안**: MySQL(익숙할 수 있으나 `ON CONFLICT DO NOTHING`·부분 인덱스가 없거나 다름), H2(설치가 필요 없지만 운영 DB와 동작이 다름)
- **근거**
  - 회사가 PostgreSQL을 쓴다고 과제에 적혀 있다
  - `INSERT ... ON CONFLICT DO NOTHING`으로 중복 이벤트를 한 문장에 처리할 수 있다
  - `timestamptz`(타임존 포함 시각)로 UTC 저장이 명확하다
  - **부분 유니크 인덱스**(`UNIQUE ... WHERE status = 'APPROVED'`)로 "주문당 승인 결제는 하나"를 DB가 보장한다
- **처음 써 봄?**: ✍️

### ADR-07. Flyway

- **결정**: `db/migration/V1__schema.sql`, `V2__seed_plans.sql`, ...
- **대안**: JPA `ddl-auto=update`(편하지만 어떤 DDL이 실행됐는지 보이지 않고, 제약 조건을 정확히 제어하기 어려움), Liquibase(XML/YAML 문법을 따로 배워야 함), 수동 SQL(재현 절차를 README에 따로 써야 함)
- **근거**: "제출 요건이 '빈 DB에서 스키마와 시드가 재현될 것'이라 앱을 띄우면 자동으로 적용되는 Flyway를 골랐습니다. SQL을 그대로 쓰기 때문에 UNIQUE 제약 같은 핵심 장치가 파일에 그대로 보입니다."
- **규칙**: 이미 커밋된 마이그레이션 파일은 고치지 않고 새 버전(V3...)을 추가한다

### ADR-08. 도메인별 패키지 + 3계층

- **결정**: `order`, `payment`, `issue` 같은 도메인별 패키지 안에 Controller, Service, Repository를 둔다
- **대안**: 헥사고날/클린 아키텍처(포트와 어댑터). 공급사 추상화 가산점에는 유리하지만 파일 수가 2~3배로 늘어난다
- **근거**: "인터뷰에서 모든 클래스를 설명할 수 있는 수준으로 유지하고 싶었습니다. 다만 공급사 호출만은 `SupplierClient` 인터페이스로 분리해서, 나중에 공급사가 둘이 되면 구현체만 추가하도록 했습니다."

### ADR-09. 모의 PG·공급사: 같은 앱, 별도 패키지, 실제 HTTP

- **결정**: `com.roampass.mock.pg`, `com.roampass.mock.supplier` 패키지에 컨트롤러를 둔다. 우리 서비스는 `RestClient`로 `APP_PUBLIC_BASE_URL/mock/...`을 **HTTP로** 호출하고, 모의 시스템도 웹훅과 콜백을 HTTP로 보낸다. 데이터는 `mock` 스키마에 둔다
- **대안**: 별도 Spring 앱(현실적이지만 배포 대상이 2개가 되고 프리티어 메모리 1GB에 JVM 2개는 빠듯함), 같은 앱에서 메서드 직접 호출(타임아웃이나 중복 콜백을 재현할 수 없음)
- **근거**: "모의 시스템을 메서드로 직접 부르면 '응답이 안 오는 상황'을 만들 수 없어서 일부러 HTTP로 돌아 나가게 했습니다. 배포 환경에서도 콜백이 실제 네트워크를 타고 돌아와야 한다는 요건(2.6)도 이렇게 충족했습니다."

### ADR-10. Spring RestClient

- **결정**: Spring 6.1의 `RestClient`. 연결 타임아웃 1초, 읽기 타임아웃 3초
- **대안**: `RestTemplate`(유지보수 모드), `WebClient`(리액티브라 동기 코드에서 `block()`이 필요해 혼란), OpenFeign(의존성과 설정 추가)
- **근거**: "동기 코드로 읽기 쉽고 Boot 3.2부터 기본으로 권장되는 클라이언트라서 골랐습니다. **타임아웃을 반드시 설정**했습니다. 설정하지 않으면 외부 서버가 응답하지 않을 때 스레드가 무한정 묶이기 때문입니다."

### ADR-11. JUnit5 + Testcontainers + Awaitility

- **결정**: 테스트용 PostgreSQL을 Docker로 띄우는 **Testcontainers**, 비동기 결과를 기다리는 **Awaitility**
- **대안**: H2 메모리 DB(빠르지만 `ON CONFLICT`·부분 인덱스·락 동작이 PostgreSQL과 달라 **동시성 테스트가 거짓으로 통과**할 수 있음), Mockito로 DB 모킹(중복 방지를 DB가 보장하는데 DB를 가짜로 바꾸면 검증할 게 없음)
- **근거**: "중복 방지를 DB 제약으로 했기 때문에 테스트도 진짜 PostgreSQL로 해야 의미가 있다고 생각했습니다." (→ [L4](learn/L4_동시성_테스트_도구.md))
- **처음 써 봄?**: ✍️

### ADR-12. 배포: AWS 프리티어 (EC2 + RDS)

- 상세는 `10_배포_계획.md`, [L5](learn/L5_AWS_프리티어_배포.md)
- **한 줄 근거**: "Render 같은 무료 PaaS는 15분간 요청이 없으면 서버가 잠들어 스케줄러가 멈춥니다. 이 과제는 스케줄러가 '결제됐는데 발급 안 된 주문'을 끝내는 구조라서 잠들지 않는 EC2를 골랐습니다."

### ADR-13. HMAC-SHA256 서명 (JDK 기본 기능)

- **결정**: `javax.crypto.Mac`으로 `HMAC-SHA256(raw body, secret)`을 계산하고 `MessageDigest.isEqual`로 비교한다(시간 공격을 막는 고정 시간 비교)
- **주의**: 서명은 **받은 원문 바이트 그대로**에 대해 계산한다. JSON을 객체로 바꿨다가 다시 문자열로 만들면 공백이나 필드 순서가 달라져서 서명이 맞지 않는다
- **근거**: 과제 부록 A 명세 그대로. 외부 라이브러리가 필요 없다

### ADR-14. PR 화면 캡처는 Playwright MCP

- **맥락**: UI를 바꾼 PR에는 전/후 화면을 남기기로 했다(`docs/12`)
- **결정**: `@playwright/mcp` 0.0.83(headless)를 Claude Code MCP로 등록해, 에이전트가 같은 데이터·뷰포트로 before/after를 찍는다
- **고려한 대안**

  | 대안 | 장점 | 버린 이유 |
  |---|---|---|
  | 캡처를 사람이 직접 | 도구가 필요 없음 | before를 찍는 시점과 데이터가 매번 달라 비교가 안 됨 |
  | 캡처를 테스트 코드로 | 완전 자동 | 바뀐 화면을 매번 테스트 코드로 쓰는 비용이 큼. 어떤 화면을 찍을지는 변경마다 달라서 에이전트가 판단하는 MCP 방식이 맞음 |
- **처음 써 봄?**: ✍️
- **한계**: 캡처는 사람이 눈으로 비교하는 용도다. 화면이 깨졌는지를 자동으로 판정하지는 않는다
