package com.ssafy.S14P21A205.game.day.entity;

import com.ssafy.S14P21A205.game.season.entity.Season;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(
        name = "day_closing_job",
        indexes = @Index(
                name = "idx_day_closing_job_retry",
                columnList = "status,next_retry_at"
        ),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_day_closing_job_season_day",
                columnNames = {"season_id", "day"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DayClosingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "day_closing_job_id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false)
    private Integer day;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DayClosingJobStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private DayClosingJob(Season season, Integer day, LocalDateTime now) {
        this.season = season;
        this.day = day;
        this.status = DayClosingJobStatus.PENDING;
        this.retryCount = 0;
        this.nextRetryAt = now;
    }

    public static DayClosingJob pending(Season season, int day, LocalDateTime now) {
        return new DayClosingJob(season, day, now);
    }

    public boolean isClaimable(LocalDateTime now) {
        return status != DayClosingJobStatus.COMPLETED
                && (nextRetryAt == null || !nextRetryAt.isAfter(now));
    }

    public void start(LocalDateTime leaseExpiresAt) {
        this.status = DayClosingJobStatus.PROCESSING;
        this.nextRetryAt = leaseExpiresAt;
    }

    public void complete(LocalDateTime now) {
        this.status = DayClosingJobStatus.COMPLETED;
        this.nextRetryAt = now;
        this.lastError = null;
    }

    public void scheduleRetry(LocalDateTime nextRetryAt, String error) {
        this.status = DayClosingJobStatus.PENDING;
        this.retryCount = retryCount == null ? 1 : retryCount + 1;
        this.nextRetryAt = nextRetryAt;
        this.lastError = truncate(error);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }
}
