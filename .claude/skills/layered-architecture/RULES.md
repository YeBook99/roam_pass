# Rule: Company Architecture Rules

## Scope

이 규칙은 Spring Boot 아키텍처 초안, 모듈 경계, 패키지 구조, API 경계, 데이터 경계, 보안 경계, 리뷰 보고서 작성에 적용된다. persistence 역할의 실제 패키지명과 컴포넌트 명칭은 같은 폴더의 `ARCHITECTURE.md` §3(패키지)·§12(네이밍)가 정한다.

각 규칙의 상세와 예시는 `ARCHITECTURE.md`에 있다. 이 파일은 어기면 안 되는 규칙만 짧게 모은 목록이다.

## Required Rules

- Controller는 Service만 호출하고, Repository나 Dao를 직접 접근하지 않는다.
- Service는 use case와 transaction boundary를 담당한다. transaction boundary는 service use case 단위로 설명한다.
- 외부 HTTP 호출은 DB 트랜잭션 안에서 하지 않는다. 외부 호출이 있는 use case는 `TransactionTemplate`으로 트랜잭션 범위를 나눈다.
- API DTO와 domain object를 분리한다.
- Entity 또는 persistence model을 API response로 직접 노출하지 않는다.
- domain logic은 Controller에 두지 않는다.
- 다른 업무 기능의 Repository를 직접 사용하지 않는다. 다른 기능은 그 기능의 Service로 호출한다.
- external integration은 infrastructure adapter(`{base}.infra.{system}`)로 격리한다.
- 민감정보를 로그에 남기지 않는다.
- common 모듈과 shared package를 경계 없는 무제한 공유 영역으로 만들지 않는다. 공통 코드는 `{base}.global` 아래 정해진 하위 패키지에만 둔다.
- circular module dependency를 금지한다.
- microservice decomposition은 독립 배포, 소유권, 확장성, 데이터 경계가 명확할 때만 제안한다.
- Layered Monolith에서 공통이 아닌 업무 기능 패키지는 기본적으로 `{feature}.controller`, `{feature}.service`, `{feature}.repository`, `{feature}.domain`, `{feature}.dto`를 사용한다.
- `api`는 API 계약 또는 URL 경로를 설명하는 용어로 사용하고, 업무 기능 패키지명으로 사용하지 않는다.
- `application layer`는 책임 개념이며, Layered Monolith의 실제 패키지명은 `service`이다.
- 계층 의존 규칙은 `ArchitectureTest`(ArchUnit)로 테스트에서 강제한다.

## Forbidden Patterns

- 구현 코드 없이 보안 판단을 단정하는 문서
- Service 위에 Facade·Manager 같은 별도 조율 계층을 만드는 것
- `util`, `helper`, `common` 같은 이름의 잡동사니 패키지
- Controller의 `@Transactional`, 클래스 레벨 `@Transactional`
