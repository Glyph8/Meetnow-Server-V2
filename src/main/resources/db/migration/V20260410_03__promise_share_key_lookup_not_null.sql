-- 3차 배포: fallback 제거 이후 lookup 컬럼 NOT NULL 전환
ALTER TABLE promise_share_key
    MODIFY COLUMN lookup_id VARCHAR(64) NOT NULL,
    MODIFY COLUMN lookup_version SMALLINT NOT NULL;
