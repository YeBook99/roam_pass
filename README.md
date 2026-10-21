# ROAMPASS

여행자용 데이터 SIM(이심·유심) 판매 서비스의 **결제 → 발급 → 어드민 확인 → 설치 추적** 흐름을 구현하는 과제입니다.

> 작성 중입니다. 지금은 **결제, 이심 발급, 설치 추적의 기본 흐름**까지 동작합니다. 정상 경로를 먼저 끝까지 만들고, 예외 처리는 결제 이후부터 하나씩 보완하는 순서로 진행하고 있습니다.

## 실행 방법

필요한 것: git, Docker

```bash
git clone https://github.com/YeBook99/roam_pass.git && cd roam_pass
cp .env.example .env
docker compose up -d --build --wait
```

확인: `curl localhost:8080/actuator/health` → `{"status":"UP"}`

정리: `docker compose down -v`

## 현재 한계 (작업 중, 보완 예정)

- **발급 재요청이 없습니다.** 공급사 발급 요청이 실패하거나 콜백이 오지 않으면 주문이 `ISSUING`에 남습니다. 재요청(최대 3회) → `ISSUE_FAILED` → 자동 환불은 다음 보완 단계에서 만듭니다
- **필수 테스트 중 T2(중복 콜백 동시성)·T3(결제됐는데 발급 실패)는 아직 없습니다.** 위 보완과 함께 추가합니다. T1(중복 결제 통지 동시성)은 있습니다
- 15분 넘게 결과를 모르는 결제(`READY`)와 결제 대기 만료(`PENDING → EXPIRED`)는 어드민·만료 단계에서 다룹니다
- **설치 추적은 공급사 조회만 씁니다.** 설치 콜백 수신, SIM별로 조회 주기를 다르게 하는 선택적 폴링, 기기에서 삭제(`DELETED`), 환불 직전 동기 조회는 보완 단계입니다. 지금은 개통 전 SIM을 모두 1분마다 조회합니다
