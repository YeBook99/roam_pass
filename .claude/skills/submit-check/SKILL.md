---
name: submit-check
description: 과제 부록 B 제출 직전 체크리스트 10개를 자동/수동으로 점검하고 결과표를 보여 준다 (시크릿 이력 검사, clean clone 재현, 필수 테스트, README 섹션, 배포 URL). "제출 전 점검", "체크리스트 확인", "/submit-check" 에 사용.
---

# 제출 직전 점검 (부록 B)

각 항목을 **실제로 확인한 결과**로 ✅ / ❌ / ⚠️(수동 확인 필요) 표를 만든다. 확인 못 한 걸 ✅로 표시하지 않는다.

| # | 항목 | 확인 방법 |
|---|---|---|
| 1 | README에 8개 질문 답 전부, 특히 Q1·Q7 | README에서 Q1~Q8 헤더와 `【교체】`/`【본인】` 잔여 표시 검색 |
| 2 | 제출 URL이 지금 접속됨 (사용자 + 어드민) | `curl -sS -o /dev/null -w '%{http_code}'` 로 프론트 `/`, `/admin/orders`, 백엔드 `/actuator/health`. 시크릿 창 확인은 사용자에게 요청 |
| 3 | README 커맨드만으로 다른 환경에서 실행 | 스크래치패드에 `git clone` → README 커맨드 그대로 실행 (DB는 docker compose) → 테스트·기동 확인 |
| 4 | `.env` / DB 비밀번호 / API 키 / 서명 키가 저장소에 없음 (과거 커밋 포함) | `git log -p --all` 에 대해 `.githooks/pre-commit` 과 같은 패턴 검색, `git log --all --name-only` 에서 `.env` 파일 검색 |
| 5 | 마이그레이션 + 시드 요금제 포함 | `backend/src/main/resources/db/migration/` 확인, 빈 DB에서 기동 후 `sim_plan` 행 수 |
| 6 | 중복 통지·중복 콜백 동시성 테스트 존재·통과 | `./gradlew test --tests '*Concurrency*'` |
| 7 | "결제됐는데 발급 실패" 테스트 존재·통과 | `./gradlew test --tests '*IssuanceFailure*'` |
| 8 | README에 모의 PG·공급사 실패/지연/중복 켜는 시연 방법 | README 섹션 + 설명된 API/화면이 실제로 있는지 코드 대조 |
| 9 | README에 배포한 곳·이유·막힌 지점·미구현 | README 섹션 확인 |
| 10 | 실제 투입 시간과 AI 사용처 | README 섹션 확인 (투입 시간 숫자는 사용자에게 확인) |

추가 점검 (감점 항목, 2.7)
- `assertTrue(true)` 류 / assert 없는 테스트: `grep -rn "assertTrue(true)\|assertThat(true)" backend/src/test`
- 서명 검증 없는 콜백 경로가 없는지: 콜백 컨트롤러 전수 확인
- 커밋 히스토리가 squash 없이 흐름이 보이는지: `git log --oneline | wc -l`, 커밋 메시지 형식 일괄 검사
- `docs/PROGRESS.md` 미완료 항목이 README "미구현"에 전부 적혀 있는지

결과표 아래에 ❌ 항목별 고칠 방법을 적고, 고칠지 사용자에게 묻는다. 점검 자체로는 아무것도 수정하지 않는다.
