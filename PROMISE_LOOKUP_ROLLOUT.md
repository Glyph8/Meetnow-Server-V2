# Promise lookup 전환 운영/연동 문서

## 1) 프론트 문의 답변

### 1-0. 현재 기준 최종 계약(즉시 안정화 단계)
- Promise/Group 조회·참여 요청은 `lookupId`(64-char lowercase hex), `lookupVersion`(현재 1)을 기본 계약으로 사용합니다.
- `encUserId`/`encGroupId`는 **호환 fallback 경로**이며, 아래 플래그가 꺼지면 사용이 중단됩니다.
  - `promise.promisekey2.fallback-enabled`
  - `group.lookup.fallback-enabled`

### 1-1. idempotent의 “동일 key” 판정 기준
- **현재 서버는 encPromiseKey 암호문 문자열 직접 비교를 idempotent 판정에 사용하지 않습니다.**
- 이유: AES-GCM 랜덤 IV로 동일 평문도 매번 다른 암호문이 되므로, 암호문 문자열 비교는 오탐(거짓 충돌)을 유발합니다.
- 현행 동작: 동일 `(promiseId, lookupId, lookupVersion)` 요청은 idempotent 성공 처리.
- 참고: 진짜 409 충돌 검출이 필요하면, FE가 평문 기반 deterministic fingerprint(예: keyHash/HMAC)를 추가 전송해야 합니다.

### 1-2. promisekey2 fallback(encUserId) 조회 방식
- 조회 우선순위:
  1. `promiseId + lookupId + lookupVersion`
  2. fallback on일 때만:
     - `promiseId + encUserId` exact match
     - 실패 시 `promiseId + 인증 userId` 기반 조회
- 따라서 fallback은 **encUserId exact match에만 의존하지 않도록 보완**되었습니다.

### 1-3. lookupId 생성 규격
- 서버 강제 규격(검증): `^[0-9a-f]{64}$`, `lookupVersion == 1`
- 서버 비목표: lookupId 원문 재생성/검증(해시 입력값 역검증) 미수행
- FE/SDK 권장 표준(v1):
  - normalize: `trim` 후 `lowercase`
  - algorithm: `HMAC-SHA256`
  - payload: `normalizedUserId`
  - output: lowercase hex(64)
- 중요: 위 생성 규격은 FE/SDK에서 고정 구현하고, 버전 변경 시 `lookupVersion`을 증가시켜 병행 운영합니다.

### 1-4. 403/409 에러 응답 계약
- 공통 에러 스키마: `ErrorResponse`
  - `code` (int): errorCode
  - `message` (string)
  - `details`: 현재 미사용(없음)
- Swagger에 `400/403/404/409` 응답 케이스를 명시했습니다.

### 1-5. fallback 종료 기준
- `promise.promisekey2.fallback-enabled=false` 전환 권장 조건:
  - 최근 7일 이동평균 `promisekey2.lookup.fallback.rate < 0.1%`
  - 최근 3일 연속 fallback hit = 0 (피크 시간대 포함)
  - `promisekey2.not_found.count` 급증 없음(전주 대비 +20% 이내)
  - FE 배포율 99%+ (lookup 필드 전송 클라이언트)
- `group.lookup.fallback-enabled=false` 전환 권장 조건:
  - 최근 7일 이동평균 `group.lookup.fallback.count / group.lookup.success.count < 0.1%`
  - 최근 3일 연속 encGroupId fallback hit = 0
  - lookup validation fail 급증 없음(전주 대비 +20% 이내)
  - FE 배포율 99%+ (group 관련 API lookup 필드 기본 전송)

---

## 2) 마이그레이션 실행/롤백

### 2-1. 순서
1. `V20260410_01__promise_share_key_lookup_nullable.sql`
2. (fallback 사용량 모니터링)
3. `V20260410_03__promise_share_key_lookup_not_null.sql`

`V20260410_02__...placeholder.sql`은 운영 절차 체크포인트 문서 역할입니다.

### 2-2. 롤백 전략
- 1차 배포 롤백: 앱 코드에서 lookup 강제 사용 중단 + fallback on 유지
- 3차 배포 전환 실패 시: NOT NULL 변경 이전 스냅샷/백업 기준으로 복구
- 공통: 스키마 변경 전 full backup + 변경 후 smoke query(`lookup null count`, 인덱스 상태) 확인

---

## 3) 메트릭 정의서

### 3-1. `promisekey2.lookup.success.rate`
- 분자: promisekey2 요청 중 encPromiseKey 반환 성공 건수
- 분모: promisekey2 전체 요청 건수
- 태그 예시: `env`, `result=success`

### 3-2. `promisekey2.lookup.fallback.rate`
- 분자: fallback 경로로 성공한 건수
- 분모: promisekey2 전체 요청 건수
- 태그 예시: `env`, `fallback_by=encUserId|userId`

### 3-3. `join.lookup.unique_conflict.count`
- 분자/값: join 중 lookup unique 충돌로 처리된 건수(현재 정책상 사실상 0 유지 기대)
- 태그 예시: `env`

### 3-4. `promisekey2.not_found.count`
- 분자/값: promisekey2 조회 실패(404) 건수
- 태그 예시: `env`

### 3-5. 알람 임계치(권장)
- fallback.rate: 5분 이동평균 5% 초과 10분 지속
- not_found.count: 전주 동시간대 대비 2배 초과
- unique_conflict.count: 0 baseline 대비 급증(1분 5건 이상)

### 3-6. `group.lookup.success.count`
- 값: group lookup 조회 성공 건수
- 태그 예시: `path=lookup|group_id|enc_group_id`

### 3-7. `group.lookup.fallback.count`
- 값: group lookup fallback 경로 성공 건수
- 태그 예시: `path=group_id|enc_group_id`

### 3-8. `group.lookup.validation.fail.count`
- 값: group lookup 검증 실패 건수
- 태그 예시: `reason=missing_lookup|invalid_lookup_version|invalid_lookup_id_format|fallback_disabled|...`

### 3-9. `group.lookup.version.count` / `promise.lookup.version.count`
- 값: lookupVersion 분포 카운트
- 태그 예시: `lookupVersion=1`

---

## 4) soft → hard fallback 전환 절차

1. soft 단계(기본): fallback-enabled=true
   - lookup 우선 조회 + fallback 허용
   - fallback/validation 지표 모니터링
2. hard 단계(전환): fallback-enabled=false
   - legacy fallback 차단
   - lookup 미전송/legacy 의존 요청은 표준 validation 에러로 응답
3. 롤백:
   - 긴급 시 fallback-enabled=true로 즉시 복구
   - 복구 후 fallback 발생 경로(클라이언트 버전/엔드포인트) 역추적

---

## 5) CodeQL 타임아웃 재실행 정책

- 기본 원칙: PR 완료 전 CodeQL 최소 1회 성공 결과 확보
- 타임아웃 시: **최소 1회 재시도**
- 재시도도 타임아웃이면:
  - 원인(러너 상태/시간대/큐 적체) 기록
  - 수동 재실행 계획과 완료 책임자 지정 후 머지 결정
