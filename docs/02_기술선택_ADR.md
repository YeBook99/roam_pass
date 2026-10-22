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
| 08 | 아키텍처 스타일 | ~~도메인별 패키지 + 3계층~~ → ADR-15로 대체 | |
| 09 | 모의 PG·공급사 위치 | **같은 앱, 별도 패키지, 실제 HTTP 호출** | 배포 1개 + 네트워크 문제 재현 |
| 10 | HTTP 클라이언트 | **Spring RestClient** | 동기 코드, 타임아웃 설정 간단 |
| 11 | 테스트 | **JUnit5 + Testcontainers** (Awaitility는 발급 단계에서) | H2로는 동시성·`ON CONFLICT` 재현 불가 |
| 12 | 배포 | **AWS 프리티어 EC2 + RDS** | 서버가 잠들지 않아 스케줄러 유지 |
| 13 | 서명 | **HMAC-SHA256 (JDK `javax.crypto`)** | 과제 명세, 외부 라이브러리 불필요 |
| 14 | PR 화면 캡처 | **Playwright MCP** | 같은 조건으로 before/after 캡처. 필수 테스트는 백엔드에서 증명 |
| 15 | 백엔드 구조 | **레이어드 아키텍처 (기능 우선 패키지 + 계층) + ArchUnit·Lombok·validation** | 모든 기능이 같은 모양, 규칙을 테스트로 강제 |
| 16 | 어드민 조회 | **`NamedParameterJdbcTemplate` + 직접 쓴 SQL** (조회 전용) | 세 테이블 JOIN + 선택 조건을 SQL 그대로 보여 줌 |
| 17 | 프론트 구조 | **기능별 폴더(`features/*`, 백엔드와 같은 이름) + 페이지는 Server, 상호작용만 Client + `server-only`** | 파일만 보고 서버/브라우저를 구분, 실수는 빌드가 잡음 |
| 18 | 백엔드 서비스 호출 | **서비스는 인터페이스 + `*ServiceImpl`, 부르는 쪽은 인터페이스만** | 기능이 밖에 내놓는 일이 한 파일에 보이고, 구현을 몰라도 부를 수 있음 |

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
  - ~~어드민 목록 필터는 `JpaSpecificationExecutor`로 만든다~~ → 여러 테이블을 묶는 어드민 조회는 ADR-16
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

- **상태**: 대체됨(ADR-15). 결제 기능까지 이 방식으로 만들었다가 레이어드 설계 기준으로 다시 정리했다
- **결정**: `order`, `payment`, `issue` 같은 도메인별 패키지 안에 Controller, Service, Repository를 둔다
- **대안**: 헥사고날/클린 아키텍처(포트와 어댑터). 공급사 추상화 가산점에는 유리하지만 파일 수가 2~3배로 늘어난다
- **근거**: "인터뷰에서 모든 클래스를 설명할 수 있는 수준으로 유지하고 싶었습니다. 다만 공급사 호출만은 `SupplierClient` 인터페이스로 분리해서, 나중에 공급사가 둘이 되면 구현체만 추가하도록 했습니다."

### ADR-09. 모의 PG·공급사: 같은 앱, 별도 패키지, 실제 HTTP

- **결정**: `com.roampass.mockpg`, `com.roampass.mocksupplier` 기능 패키지에 둔다(처음엔 `mock.pg`였지만 ADR-15의 구조 검사가 계층을 알아볼 수 있게 이름을 바꿨다). 우리 서비스는 `RestClient`로 `APP_PUBLIC_BASE_URL/mock/...`을 **HTTP로** 호출하고, 모의 시스템도 웹훅과 콜백을 HTTP로 보낸다. 데이터는 `mock` 스키마에 둔다
- **대안**: 별도 Spring 앱(현실적이지만 배포 대상이 2개가 되고 프리티어 메모리 1GB에 JVM 2개는 빠듯함), 같은 앱에서 메서드 직접 호출(타임아웃이나 중복 콜백을 재현할 수 없음)
- **근거**: "모의 시스템을 메서드로 직접 부르면 '응답이 안 오는 상황'을 만들 수 없어서 일부러 HTTP로 돌아 나가게 했습니다. 배포 환경에서도 콜백이 실제 네트워크를 타고 돌아와야 한다는 요건(2.6)도 이렇게 충족했습니다."

### ADR-10. Spring RestClient

- **결정**: Spring 6.1의 `RestClient`. 연결 타임아웃 1초, 읽기 타임아웃 3초
- **대안**: `RestTemplate`(유지보수 모드), `WebClient`(리액티브라 동기 코드에서 `block()`이 필요해 혼란), OpenFeign(의존성과 설정 추가)
- **근거**: "동기 코드로 읽기 쉽고 Boot 3.2부터 기본으로 권장되는 클라이언트라서 골랐습니다. **타임아웃을 반드시 설정**했습니다. 설정하지 않으면 외부 서버가 응답하지 않을 때 스레드가 무한정 묶이기 때문입니다."

### ADR-11. JUnit5 + Testcontainers (+ Awaitility는 발급 단계에서)

- **결정**: 테스트용 PostgreSQL을 Docker로 띄우는 **Testcontainers**, 비동기 결과를 기다리는 **Awaitility**(비동기 발급을 테스트할 때 추가한다. 결제 단계에서는 쓰는 곳이 없어 아직 넣지 않았다)
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

### ADR-15. 백엔드 구조: 레이어드 아키텍처 (기능 우선 패키지 + 계층)

- **상태**: 채택 (ADR-08 대체)
- **맥락**: 결제 기능까지 ADR-08(도메인별 패키지 안에 Controller·Service·Repository를 평평하게 둠)로 만들었다. 기능이 하나 둘일 때는 괜찮았는데, 점검해 보니 다음 문제가 있었다
  - 같은 프로젝트 안에서도 기능마다 모양이 달랐다. 요금제는 Controller가 Repository를 바로 불렀고, 결제는 `PaymentFacade`·`PaymentTxService`라는 계층이 하나 더 있었다
  - 다른 기능의 Repository를 그냥 가져다 썼다(주문 서비스 → 요금제 Repository, 결제 서비스 → 주문 Repository)
  - `common` 패키지에 상태 전이·콜백 기록 같은 업무 코드가 쌓였고, `common.status`가 `order`·`payment`를 import하고 두 기능도 다시 `common.status`를 import하는 순환이 생겼다
  - 이런 규칙이 문서에만 있고 어겨도 빌드가 그대로 통과했다
- **결정**: `.claude/skills/layered-architecture/ARCHITECTURE.md`를 백엔드 설계 기준으로 삼는다. 핵심은 다음과 같다
  - **기능 우선 + 계층**: `{기능}.{controller|service|repository|domain|dto|exception}`. 기술 공통 코드는 `global.{config|error|entity|web}`에만, 외부 연동은 `infra.{시스템}`에만 둔다
  - **의존은 위에서 아래로만**: Controller → Service → Repository. 계층을 건너뛰지 않고, 다른 기능의 데이터는 그 기능의 Service로만 다룬다
  - **트랜잭션**: Service의 public 메서드 하나가 use case 하나이자 트랜잭션 하나다. 외부 HTTP 호출이 있는 use case는 Facade를 따로 만들지 않고 `TransactionTemplate`으로 tx1 → 호출 → tx2를 코드 블록으로 나눈다
  - **에러**: 기능별 `ErrorCode` enum + `BusinessException` → `GlobalExceptionHandler`가 `ProblemDetail`(RFC 9457) + `code`로 통일한다. Spring 기본 예외(검증 실패, 헤더 누락, 잘못된 JSON)도 같은 모양으로 나간다
  - **규칙을 테스트로 강제**: `ArchitectureTest`(ArchUnit)가 계층 규칙, 다른 기능 Repository 사용 금지, 기능 사이 순환 금지, `global`의 기능 의존 금지, `@Transactional` 위치, 필드 주입 금지를 빌드마다 검사한다
- **이번에 추가한 라이브러리**

  | 라이브러리 | 왜 | 버린 대안 |
  |---|---|---|
  | ArchUnit (`archunit-junit5` 1.3.0, 테스트 전용) | 구조 규칙을 사람이 기억하지 않아도 테스트가 막는다 | 코드 리뷰로만 지키기: 이번처럼 어긴 줄 모르고 지나간다 |
  | Lombok | 생성자 주입(`@RequiredArgsConstructor`), Entity `@Getter`, `@Slf4j`의 반복 코드를 줄인다. Entity에는 `@Getter`·`@NoArgsConstructor(PROTECTED)`만 쓰고 `@Data`·`@Setter`는 쓰지 않는다 | 직접 작성: 결제까지 그렇게 했는데 getter만으로 파일이 길어져 읽을 곳이 흐려졌다 |
  | `spring-boot-starter-validation` | 요청 형식 검증을 Request DTO의 `@NotNull`·`@Valid`로 한다 | Service에서 null을 직접 검사: 형식 검증과 업무 규칙이 섞인다 |
- **바꾼 것 (이전 → 이후)**
  - `common.status.StatusTransitionService` → 조건부 UPDATE는 각 기능 Service(`OrderService.markPaid`, `PaymentConfirmService`)가 **자기 Repository로** 하고, 성공했을 때만 `history.service.StatusHistoryService`에 이력을 남긴다. 여러 기능이 함께 쓰는 업무 코드는 주인 기능을 따로 만든다는 기준(§3-3)을 따랐다
  - `common.callback` → `callback` 기능(`CallbackLogService`, `Hmac`)
  - `PaymentFacade` + `PaymentTxService` → `PaymentService.pay` 하나에서 `TransactionTemplate`으로 나눔. 승인 응답·웹훅·대사가 함께 쓰는 확정 로직은 `PaymentConfirmService`(다른 빈의 `@Transactional`)로 둔다(§7-2)
  - `payment.PgClient`(구체 클래스) → `infra.pg.PgClient`(인터페이스) + `HttpPgClient`. 타임아웃·5xx는 "결과 모름"(빈 Optional)으로, 조회 실패는 우리 예외로 바꾼다
  - `mock.pg` → `mockpg` 기능 패키지(계층 포함)
  - `@Value` 여러 곳 → `@ConfigurationProperties` record(`OrderProperties`, `PaymentProperties`, `PgProperties`, `MockPgProperties`)
  - 직접 넣던 생성 시각 → `BaseTimeEntity`(JPA Auditing, `Clock` 기준). V1·V3에 `created_at`·`updated_at`을 넣었다. 아직 develop에 합쳐지지 않은 마이그레이션이라 새 버전을 만들지 않고 파일을 직접 고쳤다(ADR-07 규칙은 "이미 적용·공유된 파일"이 대상). 로컬 DB는 `docker compose down -v`로 지우고 다시 만들어야 한다
- **문서와 다르게 하는 점 (프로젝트 규칙 우선, ARCHITECTURE.md §1)**
  - 결제·콜백 기록 행은 정적 팩토리 대신 네이티브 `INSERT ... ON CONFLICT DO NOTHING`으로 만든다. 같은 키의 중복 생성을 DB가 막게 하기 위해서다(CLAUDE.md §3-3). 네이티브 쿼리는 Auditing을 거치지 않아 `created_at`·`updated_at`을 쿼리에서 직접 넣는다
  - 모의 PG의 웹훅 발송(`MockPgWebhookSender`)은 `infra`를 거치지 않고 서비스에서 직접 HTTP를 보낸다. 모의 PG는 "외부 시스템 자신"이라 우리 쪽 연동 규칙의 대상이 아니라고 봤다
  - 테스트는 과제 필수 테스트와 `ArchitectureTest`만 둔다. 문서 §16은 기능마다 Controller·Service 테스트를 요구하지만, 이 과제에서는 설명할 코드를 늘리지 않는 쪽을 택했다. 대신 흐름은 docker compose로 직접 호출해 확인하고 커밋에 결과를 적는다
- **고려한 대안**

  | 대안 | 장점 | 버린 이유 |
  |---|---|---|
  | ADR-08 그대로 + 규칙만 문서에 추가 | 다시 짤 필요가 없음 | 지금도 규칙을 어긴 채 통과하고 있었다. 테스트로 막지 않으면 기능이 늘수록 모양이 더 갈라진다 |
  | 계층 우선(`controller/`, `service/` 아래에 모든 기능) | 처음 보기에 단순 | 결제 하나를 고칠 때 폴더 다섯 개를 오간다. 기능 사이 의존이 import에서 보이지 않는다 |
  | 헥사고날(포트와 어댑터) | 외부 연동 교체에 가장 유연 | 파일 수가 크게 늘고 설명할 개념(포트, 어댑터, 유스케이스 인터페이스)이 많아진다. 외부 연동 교체는 `infra`의 인터페이스로 충분하다 |
  | 결제 PR을 먼저 합치고 나중에 정리 | 이력이 단순 | 다음 기능(발급)이 옛 구조 위에서 시작된다. 기능이 적은 지금 바꾸는 비용이 가장 작다 |
- **근거 (1년차 말투)**: "결제까지 만들고 점검해 보니 같은 프로젝트 안에서도 기능마다 모양이 달랐고, 다른 기능의 Repository를 가져다 쓰거나 패키지끼리 서로 import하는 순환도 있었습니다. 저 혼자 규칙을 기억하는 것으로는 막을 수 없다고 생각해서, 회사 기준 문서(ARCHITECTURE.md)에 맞춰 구조를 다시 잡고 ArchUnit으로 규칙을 테스트에 넣었습니다. 이제 규칙을 어기면 빌드가 실패합니다. 기능이 결제 하나뿐일 때 바꾸는 게 가장 싸다고 판단해서 발급을 만들기 전에 바꿨습니다."
- **처음 써 봄?**: ✍️ (ArchUnit, `TransactionTemplate`, `ProblemDetail`, `@ConfigurationProperties` record 중 처음 써 본 것과 익힌 방법)
- **한계와 바꿀 조건**
  - 기능이 같은 패턴(조건부 UPDATE → 이력)을 각자 갖고 있어 코드가 조금 반복된다. 기능이 많아져 반복이 문제가 되면 공통 부분을 `history` 쪽으로 옮기는 것을 다시 검토한다
  - 기능 사이 호출(결제 → 주문)은 Service를 직접 부른다. 순환이 생길 상황이 오면 이벤트(§6.4)로 바꾼다
  - 모듈 경계가 패키지뿐이라 다른 사람이 규칙을 어기는 것은 `ArchitectureTest`가 막는 범위까지만 막힌다

### ADR-16. 어드민 조회: NamedParameterJdbcTemplate + 직접 쓴 SQL

- **상태**: 채택 (ADR-04의 "어드민 필터는 Specification" 부분을 대체)
- **맥락**: 어드민 주문 목록은 주문(`sim_order`), 그 주문의 최신 결제(`payment`), 프로필(`sim_profile`)을 함께 보여 주고, 다섯 가지 조건(주문 상태·결제 상태·설치 상태·SIM 종류·기간)을 **값이 있을 때만** 건다. 주문 상세 타임라인은 `status_history`에서 주문·결제·프로필 이력을 한 번에 모은다. 세 테이블은 각각 다른 기능의 것이라 JPA 연관관계도 없다(다른 기능 Entity는 ID로만 참조, ARCHITECTURE.md §4-8)
- **결정**
  - `admin.repository.AdminOrderQueryRepository`(ARCHITECTURE.md의 `{Feature}QueryRepository` 이름 규칙)가 `NamedParameterJdbcTemplate`으로 SQL을 실행하고 결과를 바로 응답 DTO로 만든다
  - 선택 조건은 `StringBuilder`로 `AND o.status = :orderStatus`처럼 이어 붙이고, 값은 항상 파라미터로 넘긴다
  - **조회 전용**이다. 상태 변경은 지금처럼 각 기능 Service의 JPA 네이티브 조건부 UPDATE로만 한다(CLAUDE.md §3-2)
- **고려한 대안**

  | 대안 | 장점 | 버린 이유 |
  |---|---|---|
  | JPA `Specification` | ADR-04에서 처음 정한 방식, 타입 안전 | 엔티티 하나(`SimOrder`) 기준이라, 연관관계가 없는 결제·프로필 조건을 걸려면 서브쿼리 코드를 Criteria API로 써야 한다. 제가 읽어도 어떤 SQL이 나가는지 바로 안 보였다 |
  | JPA `EntityManager.createNativeQuery` | JPA 하나로 통일 | 결과가 `Object[]`라 열을 번호로 꺼내야 하고, 시각 타입 변환을 열마다 신경 써야 한다 |
  | QueryDSL | 타입 안전 동적 쿼리 | 코드 생성 설정이 필요하고 처음 쓰는 도구가 하나 더 는다(ADR-04와 같은 이유) |
- **ADR-04와 어긋나는 점**: ADR-04에서 "JPA + JdbcTemplate 혼용"을 버렸던 이유는 *조건부 UPDATE는 JPA 네이티브 쿼리로도 되니 섞을 필요가 없다*였다. 이번엔 쓰기가 아니라 여러 테이블 읽기이고, JdbcTemplate은 `spring-boot-starter-data-jpa`에 이미 들어 있어 의존성이 늘지 않는다. 그래서 **쓰기는 JPA, 여러 테이블 읽기는 JdbcTemplate**으로 나눴다
- **근거 (1년차 말투)**: "처음엔 Specification으로 하려 했는데, 결제·프로필 테이블은 주문과 연관관계가 없어서 조건 하나 걸 때마다 서브쿼리를 Criteria로 짜야 했습니다. 어떤 SQL이 나가는지 제가 설명을 못 할 것 같아서, 조회 전용 Repository 하나에서 SQL을 직접 쓰기로 했습니다. 조건은 값이 있을 때만 `AND`를 붙이고, 값은 꼭 파라미터로 넘겨서 SQL 인젝션을 막았습니다."
- **처음 써 봄?**: ✍️ (`NamedParameterJdbcTemplate`, `LEFT JOIN LATERAL` 사용 경험 여부)
- **한계와 바꿀 조건**
  - SQL 문자열이라 컬럼 이름이 틀려도 컴파일 때 모르고 실행해야 안다. 그래서 SQL을 고치면 필터마다 직접 호출해 확인해야 한다
  - offset 페이지네이션이라 데이터가 아주 많아지면 뒤 페이지가 느려진다. 그때는 `created_at, id` 기준 커서 방식으로 바꾼다
  - 목록과 전체 개수를 쿼리 두 번으로 센다. 그 사이에 주문이 생기면 개수가 한 건 어긋날 수 있지만 운영 화면이라 허용했다

### ADR-17. 프론트 구조: 기능별 폴더 + 페이지는 Server, 상호작용만 Client

- **상태**: 채택
- **맥락**: Next.js를 처음 쓴다. App Router는 파일 하나하나가 서버에서 돌지 브라우저에서 돌지를 정해야 하는데, 규칙 없이 만들면 페이지 전체에 `'use client'`를 붙이거나 서버 전용 코드(내부 API 주소, `cookies()`)를 브라우저 코드에 섞기 쉽다. 면접에서 모든 화면을 설명해야 하므로 화면마다 모양이 같아야 한다
- **결정**: `.claude/skills/nextjs-architecture/ARCHITECTURE.md`를 프론트 설계 기준으로 삼는다. 핵심은 다음과 같다
  - **폴더**: `app/`(URL, 조립만) / `features/{plan,order,payment,issue,admin}/`(백엔드 기능 패키지와 같은 이름. `types.ts`, `server.ts`, `client.ts`, `components/`) / `components/`(공용 표시) / `lib/`(fetch·에러·시각)
  - **경계**: `page.tsx`는 항상 Server Component로 첫 데이터를 채워 보내고, 클릭·타이머가 있는 가장 작은 조각만 `'use client'`. 서버 조회 파일은 `import 'server-only'`로 브라우저에서 import하면 빌드가 실패하게 한다
  - **데이터**: fetch는 `lib/api-server.ts`·`lib/api-client.ts`로만, 전부 `cache: 'no-store'`. 에러는 백엔드 ProblemDetail을 `ApiError(code, detail)`로 바꾼다. 폴링·버튼은 TanStack Query(ADR-02)
  - **시각**: UTC 문자열을 받고 표시할 때만 `formatKst`(시간대 고정)
  - **쓰지 않는 것**: Tailwind·UI 라이브러리, Server Actions, Route Handlers, middleware, 전역 상태 라이브러리
  - **글꼴**: `next/font/google`(Next 기본 기능)로 본문은 Geist, 주문번호·SIM 번호 같은 번호만 같은 폭 글꼴(Geist Mono)을 쓴다(디자인 스킬이 정해 준 글꼴). 빌드할 때 글꼴 파일을 받아 우리 서버가 직접 주므로 화면이 Google에 요청하지 않고, 라이브러리도 늘지 않는다. 대신 빌드 환경(CI, Docker 빌드)에서 Google Fonts에 접속할 수 있어야 한다. 한글은 Geist에 없어서 시스템 한글 글꼴로 보인다
- **이번에 추가한 라이브러리**

  | 라이브러리 | 왜 | 버린 대안 |
  |---|---|---|
  | `server-only` 0.0.1 | 서버 파일을 브라우저 코드가 import하면 빌드가 실패한다. 임시 프로젝트에서 일부러 import해 보고 빌드가 실패하는 것을 확인했다 | 체크리스트로만 지키기: 어겨도 빌드가 통과하고, 내부 주소가 브라우저 번들에 들어갈 수 있다 |
- **고려한 대안**

  | 대안 | 장점 | 버린 이유 |
  |---|---|---|
  | 종류별 폴더(`components/`, `hooks/`, `api/` 아래에 모든 기능) | 처음 보기에 단순, 예제가 많음 | 결제 화면 하나를 고칠 때 폴더 세 곳을 오간다. 백엔드 기능 이름과 맞지 않아 "이 화면의 API는 어디?"를 설명하기 어렵다 |
  | 모든 코드를 `app/` 안에 (`app/checkout/_components/`) | 화면과 코드가 붙어 있음 | 어드민과 사용자 화면이 같은 주문 타입·폴러를 쓸 때 둘 중 한 화면 폴더에서 가져와야 한다 |
  | FSD(Feature-Sliced Design) | 규칙이 체계적 | 계층(shared, entities, features, widgets, pages)과 용어가 많아 화면 다섯 개짜리 과제에 비해 설명할 것이 많다 |
  | Server Actions로 쓰기 처리 | fetch 코드가 줄어듦 | 멱등키 헤더를 명시적으로 다루기 어렵고, 처음 배울 개념이 하나 더 는다(ADR-02) |
- **근거 (1년차 말투)**: "Next.js가 처음이라 '이 코드가 서버에서 도나, 브라우저에서 도나'를 파일만 보고 알 수 있게 하고 싶었습니다. 그래서 페이지는 무조건 서버, 버튼이나 폴링처럼 움직이는 조각만 브라우저로 정하고, 서버 전용 파일은 `server-only`로 막아서 실수하면 빌드가 실패하게 했습니다. 폴더는 백엔드와 같은 기능 이름으로 나눠서, 화면 하나를 백엔드 API까지 같은 이름으로 따라가며 설명할 수 있게 했습니다."
- **처음 써 봄?**: ✍️ (App Router, `server-only`, TanStack Query 중 처음 써 본 것과 익힌 방법)
- **한계와 바꿀 조건**
  - `types.ts`를 백엔드 dto를 보고 손으로 맞춘다. 틀려도 빌드가 잡지 못한다. API가 많아지면 OpenAPI로 타입을 생성하는 것을 검토한다
  - 기능 사이 import 규칙(다른 기능은 `types`만)은 빌드가 강제하지 않는다. 체크리스트와 리뷰로 지킨다
  - rewrites 주소가 빌드 때 정해진다. 배포 환경마다 이미지를 따로 빌드해야 한다

### ADR-18. 백엔드 서비스는 인터페이스를 통해 부른다

- **상태**: 채택 (ADR-15의 Service 규칙에 덧붙임)
- **맥락**: ADR-15까지는 Controller와 다른 기능이 `OrderService` 같은 **구현 클래스**를 바로 주입받았다. 기능이 늘면서 결제·발급·설치 추적이 `OrderService`를 부르는데, 그 클래스에는 public 메서드와 private 도우미, 트랜잭션 애노테이션이 섞여 있어 "주문 기능이 밖에 내놓은 일이 무엇인지"를 한눈에 보기 어려웠다
- **결정**: `@Service` 12개를 모두 **인터페이스(원래 이름) + 구현(`*ServiceImpl`)**으로 나눴다
  - 인터페이스: public 메서드 선언과 "무엇을 하나" 주석만. 예: `order/service/OrderService.java`
  - 구현: 트랜잭션(`@Transactional`)·이벤트 수신(`@TransactionalEventListener`, `@Async`)·켜고 끄기(`@ConditionalOnProperty`)와 업무 코드. 예: `order/service/impl/OrderServiceImpl.java`
  - 폴더: `service`에는 인터페이스(와 스케줄러·설정)를 모으고, 구현은 그 아래 `service/impl`에 둔다(2026-10-22 사용자 결정). service 폴더만 열면 기능이 밖에 내놓은 일이 바로 보인다
  - 부르는 쪽(Controller, 다른 기능 Service, 스케줄러)은 원래부터 `OrderService` 타입으로 받고 있어서 **코드를 고치지 않았다**. 그 이름이 인터페이스가 됐을 뿐이다
  - `ArchitectureTest`에 규칙 네 개를 넣었다: service 패키지의 `*Service`는 인터페이스여야 한다 / `*ServiceImpl`은 `service.impl` 폴더에만 있다 / `*ServiceImpl`은 자기 자신 말고 아무도(다른 구현 포함) 직접 쓰면 안 된다 / `@Service`는 `*ServiceImpl`에만 붙는다. 일부러 Controller가 구현을 직접 쓰게 바꿔 보고 검사가 실패하는 것을 확인했다
  - 대상 아님: 스케줄러(부르는 쪽이 없음), 모의 발송기(`@Component`), 설정 묶음(`*Properties`), 바깥 연동(`infra.*Client`는 원래 인터페이스)
- **고려한 대안**

  | 대안 | 장점 | 버린 이유 |
  |---|---|---|
  | 지금처럼 구현 클래스를 바로 주입 | 파일이 적다. 구현이 하나뿐이면 인터페이스가 군더더기라는 의견도 많다 | 다른 기능이 부를 수 있는 일과 내부 도우미가 한 파일에 섞여, 기능 경계를 설명하기 어렵다 |
  | 다른 기능이 부르는 서비스만 인터페이스로 | 파일 증가가 적다 | 서비스마다 모양이 달라진다. "모두 같은 모양"이 ADR-15의 목적이라 모든 서비스에 적용했다 |
  | `IOrderService`처럼 인터페이스에 `I`를 붙임 | 인터페이스인지 이름으로 안다 | 부르는 쪽 코드를 전부 고쳐야 한다. Spring 쪽에서는 인터페이스가 원래 이름, 구현이 `Impl`인 모양이 더 흔하다 |
- **근거 (1년차 말투)**: "결제와 발급이 주문 서비스를 부르는데, 주문 서비스 파일을 열면 내부에서만 쓰는 메서드와 트랜잭션 설정이 섞여 있어서 '주문 기능이 밖에 해 주는 일'이 바로 안 보였습니다. 그래서 인터페이스에는 밖에 내놓는 일만 주석과 함께 두고, 부르는 쪽은 인터페이스만 알게 했습니다. 구현이 하나뿐이라 과하다는 의견도 있다는 걸 알지만, 모든 기능을 같은 모양으로 두는 쪽이 설명하기 쉽다고 판단했고, 규칙은 ArchUnit 검사로 지키게 했습니다."
- **처음 써 봄?**: ✍️
- **한계와 바꿀 조건**
  - 파일 수가 서비스만큼 늘었다(12개). 구현이 하나뿐인 인터페이스가 대부분이다
  - 메서드 주석이 인터페이스와 구현에 두 벌 있다(나눌 때 원래 주석을 구현에 그대로 두고 인터페이스에 복사했다. 주석 안에 "무엇"과 "어떻게"가 섞여 있어 기계적으로 가르지 않았다). 메서드를 고칠 때는 두 곳 주석을 같이 고친다. 새로 만드는 서비스는 "무엇"을 인터페이스에, "어떻게"를 구현에 쓴다
  - 같은 서비스 안에서 자기 메서드를 부르면 `@Transactional`이 안 걸리는 문제(§6.2)는 인터페이스와 상관없이 그대로다

