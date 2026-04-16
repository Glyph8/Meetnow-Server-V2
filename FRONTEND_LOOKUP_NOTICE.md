# FE 공유: lookup 계약 전환 안내

## 1. 결정사항
- 백엔드는 lookup 제안을 수용했고, 조회/참여 식별 계약을 `lookupId + lookupVersion` 중심으로 단일화합니다.
- `encUserId`/`encGroupId`는 호환 기간 동안만 fallback으로 허용합니다.

## 2. FE 즉시 액션
- Promise/Group 대상 요청에 `lookupId`, `lookupVersion`을 기본 전송해 주세요.
- `lookupId`는 `^[0-9a-f]{64}$` 포맷으로 전달해 주세요.
- `lookupVersion`은 현재 `1`을 사용해 주세요.
- legacy 필드(`encUserId`/`encGroupId`)는 비상 호환 용도로만 유지해 주세요.

## 3. 오류 응답 계약
- 공통 에러 스키마: `code`, `message`
- lookup 관련 주요 실패:
  - invalid format: `LOOKUP_INVALID_FORMAT` (400)
  - unsupported version: `LOOKUP_VERSION_UNSUPPORTED` (400)
  - legacy fallback disabled: `LOOKUP_LEGACY_FALLBACK_DISABLED` (400)

## 4. 공동 검증 시나리오(스테이징)
- 정상 lookup 요청
- lookupId 형식 오류
- 미지원 lookupVersion
- legacy fallback 요청
- fallback 종료 후 legacy 요청(예상: 400)

## 5. 운영 전환
- fallback 사용률이 임계치 이하로 안정화되면
  - `promise.promisekey2.fallback-enabled=false`
  - `group.lookup.fallback-enabled=false`
  로 전환합니다.

## 6. 중기 계획(subjectId)
- 중기적으로 서버 발급 `subjectId`(opaque) 기반 전환을 진행합니다.
- 전환 이후 FE는 식별자 전달만 담당하고, lookup 계산 책임은 서버 내부 매핑으로 흡수됩니다.
