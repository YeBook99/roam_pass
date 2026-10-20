# templates

`ARCHITECTURE.md`의 규칙을 코드로 옮긴 틀이다. 복사한 뒤 자리표시자를 바꾸고, 예시 필드(`code`, `name`)는 기능에 맞게 고친다.
틀과 `ARCHITECTURE.md`가 다르면 **문서가 기준**이다. 틀을 문서에 맞게 고친다.

## 자리표시자

| 자리표시자 | 뜻 | 예 |
|---|---|---|
| `{{base}}` | 기본 패키지 | `com.example.shop` |
| `{{feature}}` / `{{Feature}}` / `{{FEATURE}}` | 기능 이름 (camel / Pascal / UPPER_SNAKE) | `product` / `Product` / `PRODUCT` |
| `{{features}}` | URL 경로용 복수형(kebab-case) | `products` |
| `{{featureLabel}}` | 에러 메시지에 쓸 한글 이름 | `상품` |
| `{{Entity}}` / `{{entity}}` | Entity 이름 (기본은 기능 이름과 같음) | `Product` / `product` |
| `{{table}}` | 테이블 이름 (snake_case, 단수) | `product` |
| `{{Action}}` / `{{action}}` | 외부 호출 use case 동작 | `Sync` / `sync` |
| `{{system}}` / `{{System}}` | 외부 시스템 이름 | `pg` / `Pg` |
| `{{bootVersion}}`, `{{javaVersion}}`, `{{group}}`, `{{appName}}` | 빌드 설정. 프로젝트 CLAUDE.md의 스택을 따른다 | `3.3.13`, `17` |

## 파일 → 만들 위치

| 틀 | 위치 (`src/main/java/{{base}}` 기준) | 모드 |
|---|---|---|
| `build/build.gradle.tmpl` | `build.gradle` (이미 있으면 빠진 의존성만 추가) | init |
| `build/application.yml.tmpl` | `src/main/resources/application.yml` (이미 있으면 빠진 키만 추가) | init |
| `init/main/global/config/*` | `global/config/` | init |
| `init/main/global/error/*` | `global/error/` | init |
| `init/main/global/entity/*` | `global/entity/` | init |
| `init/main/global/web/*` | `global/web/` (페이지 응답이 생길 때) | init |
| `init/test/ArchitectureTest.java.tmpl` | `src/test/java/{{base}}/ArchitectureTest.java` | init |
| `init/test/support/IntegrationTestSupport.java.tmpl` | `src/test/java/{{base}}/support/` | init |
| `feature/main/controller/Controller` | `{{feature}}/controller/{{Feature}}Controller.java` | feature |
| `feature/main/service/Service` | `{{feature}}/service/{{Feature}}Service.java` | feature |
| `feature/main/service/ExternalCallService` | `{{feature}}/service/{{Feature}}{{Action}}Service.java` (외부 호출이 있을 때) | feature |
| `feature/main/repository/Repository` | `{{feature}}/repository/{{Entity}}Repository.java` | feature |
| `feature/main/repository/QueryRepository` | `{{feature}}/repository/{{Feature}}QueryRepository.java` (조인 조회 화면이 있을 때) | feature |
| `feature/main/domain/Entity` | `{{feature}}/domain/{{Entity}}.java` | feature |
| `feature/main/dto/*` | `{{feature}}/dto/` | feature |
| `feature/main/exception/ErrorCode` | `{{feature}}/exception/{{Feature}}ErrorCode.java` | feature |
| `feature/test/controller/ControllerTest` | `src/test/java/{{base}}/{{feature}}/controller/` | feature |
| `feature/test/service/ServiceTest` | `src/test/java/{{base}}/{{feature}}/service/` | feature |
| `infra/*` | `infra/{{system}}/` (외부 연동이 처음 생길 때) | feature |

외부 연동(`infra/*`)을 추가하면 `application.yml`에 `external.{{system}}.base-url`, `connect-timeout`, `read-timeout`을 함께 넣는다. 값이 없으면 앱이 뜨지 않는다.

마이그레이션은 틀이 없다. `src/main/resources/db/migration/V{n}__create_{{table}}.sql`을 Entity와 같은 컬럼으로 직접 쓴다(`created_at`, `updated_at` 포함).
