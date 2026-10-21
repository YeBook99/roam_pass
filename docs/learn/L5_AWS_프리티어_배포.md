# L5. AWS 프리티어 배포 가이드 — EC2 + RDS + docker compose + Caddy

> 목표: 돈을 쓰지 않고 과제를 배포하고, 그 과정을 README "배포한 곳 / 고른 이유 / 막혔던 지점"에 쓸 수 있게 된다
> 결정과 근거는 `10_배포_계획.md`를 본다. 이 문서는 **절차와 주의점**만 다룬다
> ⚠️ AWS 요금과 정책은 자주 바뀐다. 아래 내용은 2026년 10월 작성 시점 기준이므로, **각 단계에서 콘솔의 "Free tier eligible" 표시와 Billing 화면을 직접 확인**한다

---

## 0. 한 줄 비유

> - **EC2** = 빌린 컴퓨터 한 대(리눅스)
> - **RDS** = AWS가 관리해 주는 PostgreSQL. 백업과 패치는 AWS가 한다
> - **보안 그룹** = 컴퓨터 앞의 경비원. 허락된 포트와 출처만 들여보낸다
> - **Caddy** = 현관 안내원. HTTPS 인증서를 알아서 받아 오고 요청을 프론트와 백엔드로 나눠 보낸다

---

## 1. 시작 전에: 내 계정은 어떤 무료 조건인가 (가장 중요)

AWS 무료 정책은 **계정을 만든 날짜**에 따라 다르다.

| 계정 생성 시점 | 적용 정책 | 핵심 |
|---|---|---|
| **2025-07-15 이후** | **Free plan (크레딧 방식)** | 가입 시 $100 크레딧, 활동(EC2 실행, RDS 생성, Budget 설정 등)으로 최대 $100 추가. **6개월이 지나거나 크레딧을 다 쓰면 무료 플랜이 끝난다.** 유료로 전환하기 전까지는 과금되지 않는다 |
| 2025-07-15 이전 | 기존 프리티어 (12개월) | EC2 micro 750시간/월, RDS micro 750시간/월 등 서비스별 무료 한도 |

출처: [AWS 공식 발표 (2025-07)](https://aws.amazon.com/about-aws/whats-new/2025/07/aws-free-tier-credits-month-free-plan/), [Neowin 정리](https://www.neowin.net/news/aws-free-tier-overhauled-new-users-get-200-in-credits-but-theres-a-catch/)

✍️ 내 계정 확인 결과: ________ (Billing → Free tier 화면에서 확인)

**과제 기간(7일)은 어느 쪽이든 무료 범위 안이다.** 다만 아래 §2의 과금 함정은 정책과 무관하게 조심해야 한다.

## 2. 과금 함정 체크리스트 (먼저 읽기)

- [ ] **Budget 알림부터 설정한다.** Billing → Budgets → "Zero spend budget" 또는 월 $1 예산을 만들고 이메일 알림을 켠다. 새 계정은 이것만으로 크레딧이 추가되는 활동에 해당한다
- [ ] **리전은 하나만** 쓴다(서울 `ap-northeast-2`). 다른 리전에 실수로 만든 리소스는 찾기 어렵다
- [ ] **퍼블릭 IPv4 주소는 2024년부터 시간당 과금 대상**이다(무료 한도나 크레딧으로 상쇄되는지 Billing에서 확인). 인스턴스는 1대만 둔다
- [ ] **Elastic IP를 만들고 인스턴스에 연결하지 않은 채 두면 과금**된다
- [ ] RDS: **Multi-AZ 끄기**, 스토리지 **20GB**, 스토리지 자동 확장 끄기, 템플릿은 "Free tier" 또는 "Sandbox"
- [ ] EC2 디스크(EBS)는 **30GB 이하**
- [ ] NAT Gateway, Load Balancer는 **만들지 않는다**(시간당 과금, 이 과제에 불필요)
- [ ] 과제가 끝나면 §9 정리 절차를 따른다

## 3. 전체 구성

```
[인터넷] ──443/80──▶ [EC2 보안그룹: 22(내 IP), 80, 443]
                     EC2 (Amazon Linux 2023, t3.micro 또는 t2.micro)
                     └ docker compose
                        ├ caddy     :80/:443 → HTTPS, 라우팅
                        ├ frontend  :3000 (Next.js standalone)
                        └ backend   :8080 (Spring Boot + 모의 PG·공급사)
                              │ 5432
                              ▼
                     [RDS 보안그룹: 5432 ← EC2 보안그룹만]
                     RDS PostgreSQL (db.t3.micro 또는 db.t4g.micro, 퍼블릭 액세스 없음)
```

## 4. 단계별 절차

### 4.1 보안 그룹 두 개
| 이름 | 인바운드 규칙 | 이유 |
|---|---|---|
| `roampass-ec2-sg` | SSH 22: **내 IP만** / HTTP 80: 0.0.0.0/0 / HTTPS 443: 0.0.0.0/0 | 80은 Let's Encrypt 인증서 발급과 HTTPS 리다이렉트에 필요 |
| `roampass-rds-sg` | PostgreSQL 5432: **출처 = `roampass-ec2-sg`** | IP가 아니라 "EC2 보안 그룹"을 출처로 지정 → EC2에서만 DB 접근 가능 |

> 8080, 3000 포트는 **열지 않는다.** 외부에서는 Caddy(443)를 통해서만 들어온다.

### 4.2 RDS PostgreSQL 만들기
1. RDS → 데이터베이스 생성 → **표준 생성** → PostgreSQL 16
2. 템플릿: **프리 티어**(또는 Free plan에서 허용되는 최소 사양)
3. DB 인스턴스 식별자 `roampass-db`, 마스터 사용자 `roampass`, 비밀번호는 **비밀번호 관리자에만 저장**(저장소에 절대 넣지 않음)
4. 인스턴스 클래스: `db.t3.micro` 또는 `db.t4g.micro`
5. 스토리지 20GB, **자동 확장 끄기**
6. 연결: **퍼블릭 액세스 아니요**, VPC 보안 그룹은 `roampass-rds-sg`
7. 추가 구성: 초기 데이터베이스 이름 `roampass`
8. 생성 후 **엔드포인트**(예: `roampass-db.xxxx.ap-northeast-2.rds.amazonaws.com`)를 복사해 둔다

### 4.3 EC2 만들기
1. EC2 → 인스턴스 시작 → **Amazon Linux 2023**
2. 인스턴스 유형: "Free tier eligible" 표시가 있는 `t3.micro` 또는 `t2.micro`
3. 키 페어 생성(`roampass-key.pem`). 다운로드한 파일은 **저장소 폴더 밖에** 보관한다
4. 네트워크: 퍼블릭 IP 자동 할당 **활성화**, 보안 그룹 `roampass-ec2-sg`
5. 스토리지 20~30GB gp3

### 4.4 EC2 초기 설정 (SSH 접속 후)
```bash
ssh -i roampass-key.pem ec2-user@<EC2 퍼블릭 IP>

# Docker 설치
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user        # 다시 로그인해야 적용됨

# docker compose 플러그인 (AL2023 기본 저장소에 없음 → 바이너리 직접 설치)
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64 \
     -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
docker compose version

# 스왑 2GB (메모리 1GB 보완: JVM + Node가 함께 돌면 빠듯함)
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
free -h
```

### 4.5 이미지는 EC2에서 빌드하지 않는다
**이유**: Gradle 빌드와 Next.js 빌드는 메모리를 많이 쓴다. 1GB 서버에서 빌드하면 멈추거나 매우 느리다.

방법 A — GitHub 컨테이너 레지스트리(GHCR, 무료):
```bash
# 내 PC에서 (Apple Silicon Mac이면 --platform 필수: EC2는 x86_64)
docker buildx build --platform linux/amd64 -t ghcr.io/<내계정>/roampass-backend:latest ./backend --push
docker buildx build --platform linux/amd64 -t ghcr.io/<내계정>/roampass-frontend:latest ./frontend --push
# EC2에서
docker login ghcr.io      # GitHub 토큰(read:packages)
docker compose -f docker-compose.prod.yml pull
```

방법 B — 레지스트리 없이 파일로 전송:
```bash
docker save roampass-backend:latest | gzip | ssh -i roampass-key.pem ec2-user@<IP> 'gunzip | docker load'
```

### 4.6 서버의 `.env` (저장소에 넣지 않음)
```bash
# EC2의 ~/roampass/.env  — 이 파일은 서버에만 존재
SITE_DOMAIN=3-35-12-34.sslip.io              # EC2 퍼블릭 IP의 점(.)을 하이픈(-)으로
APP_PUBLIC_BASE_URL=https://3-35-12-34.sslip.io
SPRING_DATASOURCE_URL=jdbc:postgresql://<RDS 엔드포인트>:5432/roampass
SPRING_DATASOURCE_USERNAME=roampass
SPRING_DATASOURCE_PASSWORD=<비밀번호>
SUPPLIER_HMAC_SECRET=<openssl rand -hex 32 결과>
PG_HMAC_SECRET=<openssl rand -hex 32 결과>
JAVA_TOOL_OPTIONS=-Xmx384m -XX:MaxMetaspaceSize=128m
```
- 저장소에는 같은 키 이름에 값만 비운 **`.env.example`**을 커밋한다
- `chmod 600 .env`로 다른 사용자가 읽지 못하게 한다

### 4.7 Caddy 설정 (HTTPS 자동)
```
# Caddyfile
{$SITE_DOMAIN} {
    handle /api/*  { reverse_proxy backend:8080 }
    handle /mock/* { reverse_proxy backend:8080 }
    handle         { reverse_proxy frontend:3000 }
}
```
- **sslip.io**: `3-35-12-34.sslip.io` 같은 이름을 IP `3.35.12.34`로 연결해 주는 무료 DNS다. 도메인을 사지 않아도 Caddy가 Let's Encrypt 인증서를 자동으로 받는다
- 인증서 발급에는 **80, 443 포트가 열려 있어야** 한다

### 4.8 docker-compose.prod.yml (개념)
```yaml
services:
  caddy:
    image: caddy:2
    ports: ["80:80", "443:443"]
    volumes: ["./Caddyfile:/etc/caddy/Caddyfile", "caddy_data:/data"]
    env_file: .env
    restart: unless-stopped
  backend:
    image: ghcr.io/<내계정>/roampass-backend:latest
    env_file: .env
    restart: unless-stopped          # 서버 재부팅·장애 시 자동 재시작 → 스케줄러 복구
  frontend:
    image: ghcr.io/<내계정>/roampass-frontend:latest
    environment:
      API_INTERNAL_URL: http://backend:8080
    restart: unless-stopped
volumes:
  caddy_data:                        # 인증서 보관 (재시작해도 재발급 안 하도록)
```

### 4.9 기동과 확인
```bash
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml logs -f backend   # Flyway 마이그레이션, 시작 로그 확인

curl -s https://<SITE_DOMAIN>/api/plans | head          # 요금제 시드가 나오면 DB 연결 OK
curl -s https://<SITE_DOMAIN>/actuator/health           # (노출했다면) UP
```
**콜백이 실제로 돌아오는지 확인**(2.6 필수): 화면에서 이심을 하나 결제한 뒤, 어드민 주문 상세 타임라인에 "공급사 콜백 수신 · 서명 OK"가 찍히는지 본다. 로그에서는 `POST /api/webhooks/supplier`를 확인한다.

### 4.10 시크릿 창에서 최종 확인 (부록 B)
- 다른 기기나 시크릿 창에서 사용자 화면(`/`)과 어드민(`/admin`)을 열어 본다
- README에 URL을 적는다

## 5. 재시작과 IP 변경 주의

- EC2를 **중지 후 시작**하면 퍼블릭 IP가 바뀐다 → sslip.io 도메인이 바뀐다 → `.env`, README URL, 인증서가 모두 바뀐다
- 제출 기간에는 **중지하지 말고 계속 켜 둔다**(재부팅은 IP가 유지됨)
- 고정 IP가 필요하면 Elastic IP를 쓸 수 있지만 IPv4 과금 대상이므로 Billing에서 확인한다

## 6. 자주 막히는 지점과 해결

| 증상 | 확인할 것 |
|---|---|
| SSH 접속 안 됨 | 보안 그룹 22번의 "내 IP"가 바뀌었는지(카페·집 이동), 키 파일 권한 `chmod 400` |
| Spring이 DB 연결 타임아웃 | RDS 보안 그룹 출처가 **EC2 보안 그룹**인지, 엔드포인트 오타, RDS가 "사용 가능" 상태인지 |
| `exec format error` | Mac(ARM)에서 빌드한 이미지 → `--platform linux/amd64`로 다시 빌드 |
| 컨테이너가 계속 재시작 / `Killed` | 메모리 부족 → swap 확인(`free -h`), `-Xmx` 낮추기 |
| HTTPS 인증서 발급 실패 | 80/443 인바운드, `SITE_DOMAIN`이 현재 IP와 일치하는지 |
| 콜백이 안 옴 | `APP_PUBLIC_BASE_URL`이 localhost로 남아 있지 않은지. backend 컨테이너 안에서 `curl $APP_PUBLIC_BASE_URL/api/plans` |
| 만료 시각이 9시간 다름 | 서버 타임존은 UTC다. 코드가 `Asia/Seoul`을 명시하는지(`07` §4) |

> ✍️ **README "막혔던 지점"은 실제로 겪은 것만 쓴다.** 형식: `증상 → 처음 의심한 것 → 실제 원인 → 해결(참고한 문서/AI) → 걸린 시간`

## 7. 무료 플랜이 끝나기 전에 (6개월 또는 크레딧 소진)

- 새 계정의 Free plan은 6개월이 지나거나 크레딧을 다 쓰면 끝난다. 이후 계정 처리 방식(유료 전환 여부, 리소스 유지)은 **Billing 화면의 안내를 직접 확인**한다
- 과제 평가가 끝나면 §9로 정리한다

## 8. (가산점) push하면 배포까지

GitHub Actions 흐름은 이렇다.
`test → docker buildx (linux/amd64) → GHCR push → SSH로 EC2 접속 → docker compose pull && up -d`

- SSH 키와 서버 주소는 **GitHub Secrets**에 넣는다
- 빌드를 Actions에서 하므로 EC2 메모리 문제도 해결된다

## 9. 정리 (과금 방지)

1. EC2 인스턴스 **종료(terminate)**. 중지가 아니라 종료해야 디스크(EBS)도 함께 삭제된다(설정 확인)
2. RDS 삭제. 최종 스냅샷이 필요 없으면 "생성 안 함"을 고른다(스냅샷도 저장 비용이 있음)
3. Elastic IP를 만들었다면 **릴리스**
4. 보안 그룹과 키 페어 삭제
5. 다음 날 Billing → Bills에서 0원인지 확인

## 10. 면접에서 말하는 법

> "AWS 프리티어로 EC2 한 대에 docker compose로 프론트, 백엔드, Caddy를 올리고 DB는 RDS를 썼습니다. Render 같은 무료 PaaS는 15분 동안 요청이 없으면 서버가 잠드는데, 제 설계는 스케줄러가 결제 후 발급 안 된 주문을 끝내는 구조라 잠들지 않는 서버가 필요했습니다.
> RDS는 퍼블릭 접근을 끄고 EC2 보안 그룹에서만 접속하게 했고, HTTPS는 도메인을 사지 않으려고 sslip.io와 Caddy로 해결했습니다. 콜백 주소는 환경변수로 공개 주소를 받아서 모의 공급사가 실제 인터넷 경로로 콜백하도록 했습니다.
> 막혔던 건 ✍️ ______였습니다."

### 스스로 점검
- [ ] RDS 보안 그룹의 출처를 IP가 아니라 EC2 보안 그룹으로 지정한 이유를 말할 수 있다
- [ ] EC2에서 이미지를 빌드하지 않은 이유를 말할 수 있다
- [ ] 콜백 주소를 어떻게 정했고 배포 환경에서 콜백이 오는지 어떻게 확인했는지 말할 수 있다
- [ ] 서버가 재시작되면 스케줄러 작업이 어떻게 복구되는지 말할 수 있다
- [ ] 내 계정이 어떤 무료 정책인지 확인했고 Budget 알림을 켰다
