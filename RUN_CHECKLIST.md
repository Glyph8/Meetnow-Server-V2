# Meetnow Server 실행 체크리스트 (Local / EC2)

## 1) 사전 확인
- [ ] 저장소 경로: `<repository-root>`
- [ ] Java 17 설치 확인
- [ ] Docker / Docker Compose 사용 가능

## 2) 로컬 실행 체크리스트
### 2-1. 환경변수 파일 준비
- [ ] `<repository-root>/.env.local.example` 복사
```bash
cd <repository-root>
cp .env.local.example .env.local
```
- [ ] `.env.local`에서 OAuth/AWS/AI 값 실제 값으로 수정
- [ ] lookup 전환 플래그 확인
  - `PROMISE_KEY_FALLBACK_ENABLED=true` (호환 운영 중)
  - `GROUP_LOOKUP_FALLBACK_ENABLED=true` (호환 운영 중)

### 2-2. 의존 서비스 실행 (MySQL/Redis)
```bash
cd <repository-root>
docker compose -f docker-compose.v1.yml up -d meetnow-db meetnow-redis
```
- [ ] MySQL: `localhost:3340`
- [ ] Redis: `localhost:6390`

### 2-3. 서버 실행
```bash
cd <repository-root>
set -a
source .env.local
set +a
./gradlew bootRun
```
- [ ] 서버 포트: `8080`

### 2-4. 서버 상태 확인
```bash
curl http://localhost:8080/actuator/health
```
- [ ] `UP` 응답 확인

### 2-5. 프론트 연결 확인
- [ ] 프론트 Origin이 아래 중 하나인지 확인
  - `http://localhost:3000`
  - `https://meetnow.duckdns.org`
  - `https://next-time-together-frontend.vercel.app`
- [ ] 프론트 API Base URL = `http://localhost:8080`
- [ ] 프론트 요청 옵션 `withCredentials: true` 적용

## 3) EC2 실행 체크리스트
### 3-1. 환경변수 파일 준비
- [ ] `<repository-root>/.env.prod.example` 복사 후 값 치환
```bash
cd <repository-root>
cp .env.prod.example .env.prod
```
- [ ] DB/Redis/OAuth/AWS 실제 운영 값 입력
- [ ] lookup 전환 플래그 운영값 확인
  - `PROMISE_KEY_FALLBACK_ENABLED`
  - `GROUP_LOOKUP_FALLBACK_ENABLED`

### 3-2. EC2 보안그룹 확인
- [ ] `22` (SSH) 허용
- [ ] `8080` (직접 접근 시) 또는 `80/443` (Nginx 리버스프록시 시) 허용
- [ ] DB/Redis 포트 외부 미공개 확인

### 3-3. 백엔드 이미지 빌드/실행
```bash
cd <repository-root>
docker build -f Dockerfile.deploy -t meetnow-server:latest .
docker run -d --name meetnow-server -p 8080:8080 --env-file .env.prod meetnow-server:latest
```

### 3-4. 운영 상태 확인
```bash
docker logs -f meetnow-server
curl http://<ec2-public-ip>:8080/actuator/health
```
- [ ] `UP` 응답 확인

### 3-5. 프론트 연동 확인
- [ ] 프론트 API Base URL = `http(s)://<ec2-domain-or-ip>:8080` 또는 리버스프록시 도메인
- [ ] OAuth Redirect URI가 `.env.prod`와 프론트 설정에서 정확히 일치
- [ ] CORS 허용 Origin 목록에 프론트 도메인이 포함되어 있는지 확인

## 4) 문제 발생 시 빠른 점검
- [ ] 서버 부팅 실패: `.env.local`/`.env.prod` 누락 키 확인
- [ ] 401/토큰 문제: `Authorization` 헤더 및 refresh-token 흐름 확인
- [ ] CORS 문제: 프론트 Origin과 `CorsConfig` 허용 목록 비교
- [ ] OAuth 로그인 실패: Provider 콘솔 Redirect URI와 서버/프론트 값 일치 확인
