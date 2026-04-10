-- 1차 배포: lookup 컬럼 nullable 추가 + 조회/유니크 인덱스 추가
ALTER TABLE promise_share_key
    ADD COLUMN IF NOT EXISTS lookup_id VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS lookup_version SMALLINT NULL,
    ADD COLUMN IF NOT EXISTS user_id VARCHAR(255) NULL;

CREATE UNIQUE INDEX uk_promise_lookup
    ON promise_share_key (promise_id, lookup_version, lookup_id);
