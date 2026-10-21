# L5. AWS 프리티어 배포 가이드 — EC2 한 대 + docker compose(DB 포함) + Caddy

> 목표: 돈을 쓰지 않고 과제를 배포하고, 그 과정을 README "배포한 곳 / 고른 이유 / 막혔던 지점"에 쓸 수 있게 된다
> 결정과 근거는 `10_배포_계획.md`를 본다. 이 문서는 **절차와 주의점**만 다룬다
> ⚠️ AWS 요금과 정책은 자주 바뀐다. 아래 내용은 2026년 10월 작성 시점 기준이므로, **각 단계에서 콘솔의 "Free tier eligible" 표시와 Billing 화면을 직접 확인**한다

---

## 0. 한 줄 비유

> - **EC2** = 빌린 컴퓨터 한 대(리눅스)
> - **docker compose** = 컨테이너 여러 개(Caddy, 화면, 서버, DB)를 파일 하나로 같이 띄우는 도구. 로컬과 같은 방식이다
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
- [ ] EC2 디스크(EBS)는 **30GB 이하**
- [ ] NAT Gateway, Load Balancer는 **만들지 않는다**(시간당 과금, 이 과제에 불필요)
- [ ] 과제가 끝나면 §9 정리 절차를 따른다

## 3. 전체 구성

```
[인터넷] ──443/80──▶ [EC2 보안그룹: 22(내 IP), 80, 443]
                     EC2 (Ubuntu 24.04, t3.micro 또는 t2.micro, swap 2GB)
                     └ docker compose -f docker-compose.prod.yml
                        ├ caddy     :80/:443 → HTTPS, 라우팅 (밖으로 열린 유일한 컨테이너)
                        ├ frontend  :3000 (Next.js standalone)
                        ├ backend   :8080 (Spring Boot + 모의 PG·공급사)
                        └ postgres  :5432 (데이터는 볼륨 pgdata에 남는다)
```
- RDS는 쓰지 않는다(ADR-12). DB도 로컬과 똑같이 compose의 컨테이너다
- 3000, 8080, 5432는 **밖으로 열지 않는다.** 컨테이너끼리는 compose 내부망에서 서비스 이름(`backend`, `postgres`)으로 부른다

## 4. 단계별 절차

### 4.1 보안 그룹 하나
| 이름 | 인바운드 규칙 | 이유 |
|---|---|---|
| `roampass-ec2-sg` | SSH 22: **내 IP만** / HTTP 80: 0.0.0.0/0 / HTTPS 443: 0.0.0.0/0 | 80은 Let's Encrypt 인증서 발급과 HTTPS 리다이렉트에 필요 |

### 4.2 EC2 만들기
1. EC2 → 인스턴스 시작 → **Ubuntu 24.04** (또는 Amazon Linux 2023: 그때는 계정이 `ec2-user`, 설치는 `dnf`)
2. 인스턴스 유형: "Free tier eligible" 표시가 있는 `t3.micro` 또는 `t2.micro`
3. 키 페어 생성(`roampass-key.pem`). 다운로드한 파일은 **저장소 폴더 밖에** 보관한다
4. 네트워크: 퍼블릭 IP 자동 할당 **활성화**, 보안 그룹 `roampass-ec2-sg`
5. 스토리지 20~30GB gp3 (DB 데이터도 여기에 쌓인다)

### 4.3 EC2 초기 설정 (SSH 접속 후)

실제로 만든 서버는 **Ubuntu 24.04**(x86_64, 메모리 911MB, 디스크 8GB)다. 접속 계정은 `ubuntu`다(Amazon Linux라면 `ec2-user`).
```bash
ssh -i roampass-key.pem ubuntu@<EC2 퍼블릭 IP>

# Docker와 compose 플러그인: Ubuntu 공식 저장소의 패키지로 설치한다 (바이너리를 직접 내려받지 않는다)
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu          # 다시 로그인해야 적용됨
docker compose version

# 스왑 2GB (메모리 1GB 보완: JVM + Node + PostgreSQL이 함께 돈다)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
free -h

# 설치 찌꺼기 정리 (8GB 디스크라 아껴야 한다)
sudo apt-get clean

# 앱 폴더. 저장소가 private이라 git clone은 하지 않는다
mkdir -p ~/roampass/deploy
```
> 디스크 8GB 중 OS·Docker·스왑을 빼면 약 2.5GB가 남는다(2026-10-22 실제 값). 이미지 4개(약 1~1.5GB)는 들어가지만 새 이미지로 바꿀 때 빠듯하다. 콘솔 → EC2 → 볼륨 → "볼륨 수정"으로 20GB까지 늘리면 여유가 생긴다(서버를 끄지 않고 늘린 뒤 `sudo growpart /dev/nvme0n1 1 && sudo resize2fs /dev/nvme0n1p1`).

설정 파일 3개(`docker-compose.prod.yml`, `deploy/Caddyfile`, `deploy/prod.env.example`)는 **내 PC에서 scp로** 보낸다. 이후 배포부터는 CD가 같은 방식으로 배포하는 커밋의 파일을 보낸다.
```bash
# 내 PC의 저장소 루트에서 (main 브랜치)
scp -i roampass-key.pem docker-compose.prod.yml ubuntu@<IP>:~/roampass/
scp -i roampass-key.pem deploy/Caddyfile deploy/prod.env.example ubuntu@<IP>:~/roampass/deploy/
```

### 4.4 이미지는 EC2에서 빌드하지 않는다
**이유**: Gradle 빌드와 Next.js 빌드는 메모리를 많이 쓴다. 1GB 서버에서 빌드하면 멈추거나 매우 느리다.

- **CD가 한다**: main에 머지되면 `.github/workflows/cd.yml`이 `linux/amd64` 이미지를 빌드해 GHCR(`ghcr.io/yebook99/roampass-backend`, `-frontend`)에 올린다
- GHCR 패키지는 처음에 **비공개**다. **저장소의 공개 범위와는 따로**라서(첫 배포 때 저장소만 공개로 바꾸고 막혔다), 패키지 설정 화면(`https://github.com/users/<계정>/packages/container/<이름>/settings`) 맨 아래에서 바꾼다. 둘 중 하나를 한다
  - GitHub → Packages → 각 패키지 → Package settings → **Public**으로 바꾸기 (시크릿이 이미지에 없으므로 공개해도 된다)
  - 또는 EC2에서 `docker login ghcr.io` (GitHub 토큰, 권한 `read:packages`만)

### 4.5 서버의 `.env` (저장소에 넣지 않음)
```bash
cd ~/roampass
cp deploy/prod.env.example .env && chmod 600 .env   # 다른 사용자가 읽지 못하게
vi .env    # 아래처럼 채운다. 시크릿은 openssl rand -hex 32 로 만든다
```
| 키 | 값 | 쓰는 곳 |
|---|---|---|
| `SITE_DOMAIN` | `3-35-12-34.sslip.io` (EC2 퍼블릭 IP의 점을 하이픈으로) | Caddy가 인증서를 받을 이름 |
| `APP_PUBLIC_BASE_URL` | `https://3-35-12-34.sslip.io` | 모의 PG 웹훅·모의 공급사 콜백이 돌아올 공개 주소 |
| `POSTGRES_PASSWORD` | 랜덤 | postgres 컨테이너와 backend가 같은 값을 쓴다 |
| `PG_WEBHOOK_SECRET`, `SUPPLIER_CALLBACK_SECRET` | 랜덤 | HMAC 서명 키 |

> DB 비밀번호는 **처음 띄울 때** 볼륨에 저장된다. 나중에 `.env`만 바꾸면 접속이 안 된다(볼륨을 지우거나 DB에서 비밀번호를 바꿔야 함)

### 4.6 Caddy 설정 (HTTPS 자동)
`deploy/Caddyfile`을 그대로 쓴다.
```
{$SITE_DOMAIN} {
	handle /api/*  { reverse_proxy backend:8080 }
	handle /mock/* { reverse_proxy backend:8080 }
	handle         { reverse_proxy frontend:3000 }
}
```
- **sslip.io**: `3-35-12-34.sslip.io` 같은 이름을 IP `3.35.12.34`로 연결해 주는 무료 DNS다. 도메인을 사지 않아도 Caddy가 Let's Encrypt 인증서를 자동으로 받는다
- 인증서 발급에는 **80, 443 포트가 열려 있어야** 한다

### 4.7 docker-compose.prod.yml
저장소의 `docker-compose.prod.yml`을 그대로 쓴다. 핵심만 보면 이렇다.
| 서비스 | 이미지 | 메모 |
|---|---|---|
| postgres | `postgres:16-alpine` | `shared_buffers=64MB`, `max_connections=30`, 볼륨 `pgdata` |
| backend | `ghcr.io/yebook99/roampass-backend:${IMAGE_TAG:-latest}` | `JAVA_TOOL_OPTIONS=-Xmx384m`, postgres가 healthy가 된 뒤 시작 |
| frontend | `ghcr.io/yebook99/roampass-frontend:${IMAGE_TAG:-latest}` | backend가 healthy가 된 뒤 시작 |
| caddy | `caddy:2.8.4-alpine` | 80/443만 밖으로, 인증서는 볼륨 `caddy_data` |

모두 `restart: unless-stopped`라 서버가 재부팅되면 다시 뜨고, 스케줄러가 DB에 남은 일(PAID, ISSUING 등)을 이어서 한다.

### 4.8 기동과 확인
```bash
cd ~/roampass
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --wait
docker compose -f docker-compose.prod.yml logs -f backend   # Flyway 마이그레이션, 시작 로그 확인

curl -s https://<SITE_DOMAIN>/api/plans | head          # 요금제 시드가 나오면 DB 연결 OK
# backend는 모의 PG·공급사도 공개 주소(APP_PUBLIC_BASE_URL)로 부른다. 서버 안에서 자기 공개 주소로 나갔다 들어오는지 확인
docker compose -f docker-compose.prod.yml exec backend curl -fsS https://<SITE_DOMAIN>/api/plans > /dev/null && echo "공개 주소 왕복 OK"
free -h; docker stats --no-stream                         # 메모리 확인
```
**콜백이 실제로 돌아오는지 확인**(2.6 필수): 화면에서 이심을 하나 결제한 뒤, 어드민 주문 상세 타임라인에 `주문 ISSUING → ISSUED · SUPPLIER · CALLBACK`이 찍히는지 본다. 로그에서는 `POST /api/webhooks/supplier`를 확인한다.

**백업이 필요하면** (RDS 자동 백업이 없으므로):
```bash
docker compose -f docker-compose.prod.yml exec postgres pg_dump -U roampass roampass > backup.sql
```

### 4.9 시크릿 창에서 최종 확인 (부록 B)
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
| Spring이 DB에 연결 못 함 | `docker compose ... ps`에서 postgres가 healthy인지, `.env`의 `POSTGRES_PASSWORD`를 처음 띄운 뒤 바꾸지 않았는지(§4.5) |
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
`docker buildx (linux/amd64) → GHCR push → (승인) → 배포 커밋의 compose·Caddyfile을 scp로 EC2에 복사 → SSH로 docker compose pull && up -d --wait → https://SITE_DOMAIN/api/plans 헬스 체크` (`.github/workflows/cd.yml`)

- SSH 키와 서버 주소는 **GitHub Secrets**에 넣는다
- 빌드를 Actions에서 하므로 EC2 메모리 문제도 해결된다

## 9. 정리 (과금 방지)

1. EC2 인스턴스 **종료(terminate)**. 중지가 아니라 종료해야 디스크(EBS)도 함께 삭제된다(설정 확인)
2. Elastic IP를 만들었다면 **릴리스**
3. 보안 그룹과 키 페어 삭제
4. 다음 날 Billing → Bills에서 0원인지 확인

## 10. 면접에서 말하는 법

> "AWS 프리티어로 EC2 한 대에 docker compose로 Caddy, 프론트, 백엔드, PostgreSQL을 함께 올렸습니다. 로컬과 같은 구성이라 로컬에서 확인한 것이 서버에서도 그대로 동작합니다. Render 같은 무료 PaaS는 15분 동안 요청이 없으면 서버가 잠드는데, 제 설계는 스케줄러가 결제 후 발급 안 된 주문을 끝내는 구조라 잠들지 않는 서버가 필요했습니다.
> 밖으로는 Caddy의 80·443만 열고 DB·백엔드 포트는 열지 않았습니다. RDS를 안 쓴 대신 자동 백업이 없다는 한계가 있어서 필요하면 pg_dump로 백업합니다. HTTPS는 도메인을 사지 않으려고 sslip.io와 Caddy로 해결했습니다. 콜백 주소는 환경변수로 공개 주소를 받아서 모의 공급사가 실제 인터넷 경로로 콜백하도록 했습니다.
> 막혔던 건 ✍️ ______였습니다."

### 스스로 점검
- [ ] RDS 대신 DB를 컨테이너로 둔 이유와 그 대가(백업·디스크)를 말할 수 있다
- [ ] 3000·8080·5432를 밖으로 열지 않아도 동작하는 이유(compose 내부망, Caddy)를 말할 수 있다
- [ ] EC2에서 이미지를 빌드하지 않은 이유를 말할 수 있다
- [ ] 콜백 주소를 어떻게 정했고 배포 환경에서 콜백이 오는지 어떻게 확인했는지 말할 수 있다
- [ ] 서버가 재시작되면 스케줄러 작업이 어떻게 복구되는지 말할 수 있다
- [ ] 내 계정이 어떤 무료 정책인지 확인했고 Budget 알림을 켰다
