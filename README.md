# ROAMPASS

여행자용 데이터 SIM(이심·유심) 판매 서비스의 **결제 → 발급 → 어드민 확인 → 설치 추적** 흐름을 구현하는 과제입니다.

> 작성 중입니다. 지금은 실행 환경만 있습니다.

## 실행 방법

필요한 것: git, Docker

```bash
git clone https://github.com/YeBook99/roam_pass.git && cd roam_pass
cp .env.example .env
docker compose up -d --build --wait
```

확인: `curl localhost:8080/actuator/health` → `{"status":"UP"}`

정리: `docker compose down -v`
