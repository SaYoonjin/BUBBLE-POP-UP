SET @ddl := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'season_ranking_record'
              AND column_name = 'reputation_score'
        ),
        'ALTER TABLE season_ranking_record DROP COLUMN reputation_score',
        'SELECT 1'
    )
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
