-- group_proxy_user lookup 전환 1차: nullable 컬럼 추가 + 조회 unique 인덱스 조건부 추가
ALTER TABLE group_proxy_user
    ADD COLUMN IF NOT EXISTS group_id VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS lookup_id VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS lookup_version SMALLINT NULL;

SET @group_proxy_user_lookup_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'group_proxy_user'
      AND index_name = 'uk_group_proxy_user_lookup'
);

SET @group_proxy_user_lookup_unique_sql := IF(
    @group_proxy_user_lookup_unique_exists = 0,
    'ALTER TABLE group_proxy_user ADD UNIQUE INDEX uk_group_proxy_user_lookup (user_id, group_id, lookup_id, lookup_version)',
    'SELECT 1'
);

PREPARE group_proxy_user_lookup_unique_stmt FROM @group_proxy_user_lookup_unique_sql;
EXECUTE group_proxy_user_lookup_unique_stmt;
DEALLOCATE PREPARE group_proxy_user_lookup_unique_stmt;
