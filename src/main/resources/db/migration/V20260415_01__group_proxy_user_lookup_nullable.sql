-- group_proxy_user lookup 전환 1차: nullable 컬럼 추가 + 조회 인덱스 추가
ALTER TABLE group_proxy_user
    ADD COLUMN IF NOT EXISTS group_id VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS lookup_id VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS lookup_version SMALLINT NULL;

CREATE INDEX IF NOT EXISTS idx_group_proxy_user_lookup
    ON group_proxy_user (user_id, group_id, lookup_id, lookup_version);
