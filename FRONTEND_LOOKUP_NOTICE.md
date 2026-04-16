# 프론트엔드 연동 가이드: lookup 계약 전환(상세)

> 대상: Meetnow FE(Web/App)  
> 목적: Promise/Group 조회·참여 식별 계약을 `lookupId + lookupVersion` 기준으로 통일하고, legacy fallback 종료까지 무중단 이행

---

## 1) 핵심 변경 요약

- 서버 표준 식별 계약:
  - `lookupId` (필수, 정규식 `^[0-9a-f]{64}$`)
  - `lookupVersion` (필수, 현재 `1`)
- 호환(임시) 필드:
  - Promise: `encUserId` (fallback)
  - Group: `encGroupId` (fallback)
- fallback 제어 플래그:
  - `promise.promisekey2.fallback-enabled`
  - `group.lookup.fallback-enabled`
- 오류 응답 형식은 기존과 동일:
  - `{ "code": number, "message": string }`

---

## 2) FE가 반드시 지켜야 할 공통 규칙

### 2-1. lookup 필드 규격

- `lookupId`
  - 소문자 hex 64자만 허용
  - 대문자/공백/길이 불일치 금지
- `lookupVersion`
  - 현재 `1` 고정 전송

### 2-2. 필수 전송 정책

- Promise/Group의 조회·참여/키조회 관련 요청에는 **항상** `lookupId`, `lookupVersion` 포함
- legacy 필드(`encUserId`, `encGroupId`)는 fallback 기간 동안만 유지
- 신규 코드에서는 legacy만 단독으로 보내는 흐름 금지

### 2-3. 권장 생성 방식(v1)

- `lookupId`: FE/SDK에서 고정 알고리즘으로 생성 후 캐시 재사용
- 형식만 서버가 강제 검증하므로, 생성 입력 규칙은 FE 내부에서 일관되게 유지
- 버전 변경 시에는 `lookupVersion` 증가 + 병행 운영

---

## 3) 엔드포인트별 적용 포인트

아래 API는 현재 구현 기준으로 lookup 계약을 적용해야 하는 대표 경로입니다.

### 3-1. Promise

1. `POST /api/v1/promise/create1`
- 요청 필수: `groupId`, `lookupId`, `lookupVersion`
- 호환: `encGroupId`(optional)

2. `POST /api/v1/promise/join1`
- 요청 필수: `promiseId`, `encPromiseId`, `encPromiseMemberId`, `encUserId`, `encPromiseKey`, `lookupId`, `lookupVersion`

3. `POST /api/v1/promise/promisekey2`
- 요청 필수: `promiseId`, `lookupId`, `lookupVersion`
- 호환: `encUserId`(optional, fallback 기간)

### 3-2. Group

1. `POST /api/v1/group/new2`
- 요청 필수: `groupId`, `encGroupId`, `encencGroupMemberId`, `encUserId`, `encGroupKey`, `lookupId`, `lookupVersion`

2. `POST /api/v1/group/member/save`
- 요청 필수: `groupId`, `encGroupKey`, `encUserId`, `encGroupId`, `encencGroupMemberId`, `lookupId`, `lookupVersion`

3. `POST /api/v1/group/invite1`
- 요청 필수: `groupId`, `lookupId`, `lookupVersion`
- 호환: `encGroupId`(optional)

4. `POST /api/v1/group/leave1`
- 요청 필수: `groupId`, `lookupId`, `lookupVersion`
- 호환: `encGroupId`(optional)

5. `POST /api/v1/group/leave2`
- 요청 필수: `groupId`, `lookupId`, `lookupVersion`
- 호환: `encGroupId`(optional)

6. `POST /api/v1/group/edit1`
- 요청 필수: `groupId`, `lookupId`, `lookupVersion`
- 호환: `encGroupId`(optional)

---

## 4) 에러 처리 가이드(FE)

## 4-1. lookup 관련 표준 오류

- `LOOKUP_INVALID_FORMAT` (HTTP 400)
  - 원인: `lookupId` 형식 불일치 / lookup pair 누락
- `LOOKUP_VERSION_UNSUPPORTED` (HTTP 400)
  - 원인: 서버 미지원 `lookupVersion`
- `LOOKUP_LEGACY_FALLBACK_DISABLED` (HTTP 400)
  - 원인: fallback off 상태에서 legacy 의존 요청

## 4-2. FE 처리 원칙

1. 400 + lookup 코드
- 재시도 전 요청 payload 재구성
- 공통 토스트/에러뷰: “최신 앱으로 업데이트 후 다시 시도”
- 에러 로깅 시 `endpoint`, `lookupVersion`, `clientVersion` 포함

2. 403/404
- 권한/대상 없음으로 분기 처리(기존 로직 유지)

3. 409
- Promise key 충돌 시나리오(향후 fingerprint 도입 대비)로 분기만 준비

---

## 5) fallback 단계별 FE 동작

### Soft 단계 (fallback-enabled=true)
- lookup 필드 기본 전송
- legacy 필드는 구버전/비상 경로에서만 전송 허용
- fallback 성공률 모니터링

### Hard 단계 (fallback-enabled=false)
- legacy 의존 요청 즉시 실패(400)
- FE에서 legacy-only 요청 차단 필요
- 배포 전 클라이언트 버전 점유율 99% 이상 확인 권장

---

## 6) 스테이징 공동 검증 체크리스트

- [ ] 정상 lookup 요청 성공
- [ ] `lookupId` 형식 오류 요청이 400으로 실패
- [ ] 미지원 `lookupVersion` 요청이 400으로 실패
- [ ] fallback on 상태에서 legacy 요청 허용 확인
- [ ] fallback off 상태에서 legacy 요청 400 실패 확인
- [ ] OpenAPI 스키마와 FE payload 실제 전송값 일치 확인

---

## 7) FE 구현 체크리스트(실무용)

- [ ] lookup 생성 유틸 단일화(중복 구현 제거)
- [ ] 전송 직전 `lookupId` 소문자/길이 검증
- [ ] 대상 API 공통 인터셉터/헬퍼에 lookup 자동 주입
- [ ] fallback off 대응 feature toggle 준비
- [ ] 에러 코드별 사용자 메시지/재시도 UX 정의
- [ ] 스테이징 시나리오 자동화(E2E 또는 API 테스트)

---

## 8) 배포/롤백 협업 규칙

- 서버가 fallback off 전환하기 전 FE 사전 공지
- FE 릴리즈 노트에 “lookup 필드 강제 전송” 명시
- 장애 시:
  - 1차: 서버 fallback on 복구
  - 2차: 문제 클라이언트 버전 트래픽 식별
  - 3차: FE hotfix 재배포

---

## 9) 중기 전환(subjectId)

- 중기에는 서버 발급 `subjectId`(opaque) 기반으로 식별 체계를 이전 예정
- 전환 후 FE 책임:
  - 계산 로직 최소화
  - 서버 발급 식별자 전달 중심으로 단순화
