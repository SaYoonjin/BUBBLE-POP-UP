SET @ddl := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'store'
              AND column_name = 'pending_location_reserved_day'
        ),
        'ALTER TABLE store DROP COLUMN pending_location_reserved_day',
        'SELECT 1'
    )
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
