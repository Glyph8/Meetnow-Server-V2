-- group_proxy_user 중복 정리 후 user_id + group_id 유니크 보장
-- 최신 timestamp/ID를 유지하고 나머지 중복을 삭제한다.
DELETE gpu
FROM group_proxy_user gpu
JOIN group_proxy_user keep
  ON gpu.user_id = keep.user_id
 AND gpu.group_id = keep.group_id
 AND gpu.group_id IS NOT NULL
 AND (
     gpu.timestamp < keep.timestamp
     OR (gpu.timestamp = keep.timestamp AND gpu.group_proxy_id < keep.group_proxy_id)
 );

SET @group_proxy_user_user_group_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'group_proxy_user'
      AND index_name = 'uk_group_proxy_user_user_group'
);

SET @group_proxy_user_user_group_unique_sql := IF(
    @group_proxy_user_user_group_unique_exists = 0,
    'ALTER TABLE group_proxy_user ADD UNIQUE INDEX uk_group_proxy_user_user_group (user_id, group_id)',
    'SELECT 1'
);

PREPARE group_proxy_user_user_group_unique_stmt FROM @group_proxy_user_user_group_unique_sql;
EXECUTE group_proxy_user_user_group_unique_stmt;
DEALLOCATE PREPARE group_proxy_user_user_group_unique_stmt;

-- rollback (수동)
-- ALTER TABLE group_proxy_user DROP INDEX uk_group_proxy_user_user_group;

