-- group_share_key 중복 정리 후 group_id + enc_group_key 유니크 보장
DELETE gsk
FROM group_share_key gsk
JOIN group_share_key keep
  ON gsk.group_id = keep.group_id
 AND gsk.enc_group_key = keep.enc_group_key
 AND gsk.group_share_key_id < keep.group_share_key_id;

SET @group_share_key_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'group_share_key'
      AND index_name = 'uk_group_share_key_group_enc_key'
);

SET @group_share_key_unique_sql := IF(
    @group_share_key_unique_exists = 0,
    'ALTER TABLE group_share_key ADD UNIQUE INDEX uk_group_share_key_group_enc_key (group_id, enc_group_key)',
    'SELECT 1'
);

PREPARE group_share_key_unique_stmt FROM @group_share_key_unique_sql;
EXECUTE group_share_key_unique_stmt;
DEALLOCATE PREPARE group_share_key_unique_stmt;

-- rollback (수동)
-- ALTER TABLE group_share_key DROP INDEX uk_group_share_key_group_enc_key;

