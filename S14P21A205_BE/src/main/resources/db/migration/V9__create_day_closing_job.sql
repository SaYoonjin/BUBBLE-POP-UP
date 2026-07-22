CREATE TABLE day_closing_job (
    day_closing_job_id BIGINT NOT NULL AUTO_INCREMENT,
    season_id BIGINT NOT NULL,
    day INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6) NOT NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (day_closing_job_id),
    CONSTRAINT uk_day_closing_job_season_day UNIQUE (season_id, day),
    CONSTRAINT fk_day_closing_job_season
        FOREIGN KEY (season_id) REFERENCES season (season_id)
);

CREATE INDEX idx_day_closing_job_retry
    ON day_closing_job (status, next_retry_at);
