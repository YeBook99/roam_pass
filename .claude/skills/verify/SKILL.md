---
name: verify
description: 구현 단계에서 테스트·빌드·로컬 기동·시크릿 점검을 한 번에 수행하고 결과를 보고한다. "검증해줘", "테스트 돌려줘", "제출 전 점검" 요청 시 사용.
---

# 검증 루틴

## 1. 백엔드 테스트 (Docker 실행 중이어야 함: Testcontainers)
```bash
cd backend && ./gradlew test
```
- 반드시 포함되어야 하는 테스트 (과제 2.5-5)
  - 결제 웹훅 동시성: 같은 통지 N개 → payment 1건
  - 공급사 콜백 동시성: 같은 콜백 N개 → sim_profile 1개
  - 결제됐는데 발급 실패 → 자동 환불 / 환불 실패 시 이상 건
  - 만료일 UTC → KST 계산
- 실패하면 원인을 분석해 보고한다. 테스트를 지우거나 약하게 바꿔서 통과시키지 않는다

## 2. 프론트 빌드
```bash
cd frontend && npm run build
```
- Server Component에서 `useState`를 쓰는 등의 경계 오류가 여기서 드러난다

## 3. 전체 기동 스모크
```bash
docker compose up --build -d
curl -s localhost:8080/actuator/health
```
- 요금제 조회 → 주문 → 결제 → 발급 콜백 → ISSUED 까지 README의 시연 순서대로 확인한다

## 3-1. e2e 핵심 시나리오 (핵심 흐름·화면을 바꿨을 때)
```bash
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build --wait
cd e2e && npx playwright test
```
- 상세와 계약은 `/e2e` 스킬을 본다. UI를 바꿨다면 `/ui-screenshot` after 캡처도 한다

## 4. 시크릿 점검 (부록 B)
```bash
node scripts/ci/secret-scan.mjs        # CI와 같은 규칙
```
```bash
git log -p | grep -inE "password|secret|hmac|AKIA|PRIVATE KEY" | grep -v "\.env\.example" | head
git ls-files | grep -E "(^|/)\.env$"
```

## 5. 보고 형식
| 항목 | 결과 | 비고 |
|---|---|---|
| 백엔드 테스트 | n passed / m failed | 실패한 테스트 이름 |
| 프론트 빌드 | 성공/실패 | |
| 스모크 | 성공/실패 | 막힌 단계 |
| 시크릿 | 없음/발견 | |

README의 질문 3에 붙일 동시성 테스트 결과(로그)를 함께 제공한다.
