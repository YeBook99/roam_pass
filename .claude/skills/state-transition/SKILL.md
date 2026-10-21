---
name: state-transition
description: 주문/결제/프로필 상태를 추가·변경하거나 새 전이를 만들 때 따라야 하는 체크리스트. enum, 전이표, 조건부 UPDATE, status_history, 테스트, 문서, 어드민 필터를 함께 맞춘다.
---

# 상태·전이 변경 체크리스트

상태 설계는 "결제·발급 정합성"(25%)의 핵심이다. 상태 하나를 바꾸면 아래 항목이 **모두** 같이 움직여야 한다.

## 체크리스트

- [ ] **enum**: `OrderStatus` / `PaymentStatus` / `InstallStatus`에 값을 추가하고 `canTransitTo()` 허용 목록을 갱신한다
- [ ] **누가 이 상태를 끝내는가**: 이 상태에 들어온 주문을 다음 상태로 옮기는 주체(사용자 / 콜백 / 스케줄러 / 운영자)가 **최소 하나** 있는가? 없다면 주문이 영원히 멈춘다
- [ ] **멈춤 감지**: 이 상태에 오래 머무르면 어드민 이상 건에 잡히는가? (기준 시간)
- [ ] **전이 코드**: 그 기능 Service의 전이 메서드(예: `OrderService.markPaid`, `PaymentConfirmService`의 `transit`)에서 자기 Repository의 조건부 UPDATE(`WHERE status = :from`)로만 바꾼다. 다른 기능의 상태는 그 기능 Service를 부른다. 영향 행이 0이면 "이미 다른 곳에서 처리됨"으로 보고 조용히 종료한다
- [ ] **이력**: 조건부 UPDATE가 1행일 때만 같은 트랜잭션에서 `StatusHistoryService.record`로 from/to/actor/reason을 남긴다. actor는 USER / SYSTEM / SUPPLIER / PG / ADMIN:{id} 중 하나다
- [ ] **외부 호출**: 전이 전후로 PG·공급사 호출이 있다면 트랜잭션 밖에서 하는가 (tx1 → HTTP → tx2)
- [ ] **환불 가능 여부**: 이 상태에서 환불을 허용하는가? `RefundPolicy`에 반영했는가
- [ ] **테스트**: 정상 전이 1개, 허용되지 않은 전이 거부 1개, 동시 전이 시 1번만 성공하는지 확인하는 테스트(필요 시)
- [ ] **DB**: CHECK 제약이나 enum 문자열 길이가 맞는지 확인한다. Flyway 마이그레이션은 **새 버전 파일로** 추가하고 기존 파일은 수정하지 않는다
- [ ] **문서**: `docs/04_상태설계.md`의 전이표와 mermaid 다이어그램, README 상태 전이도
- [ ] **화면**: 어드민 상태 필터와 사용자 화면의 상태 문구
- [ ] **인터뷰**: `docs/INTERVIEW_GUIDE.md` Q2(상태 설계) 답이 여전히 맞는가
