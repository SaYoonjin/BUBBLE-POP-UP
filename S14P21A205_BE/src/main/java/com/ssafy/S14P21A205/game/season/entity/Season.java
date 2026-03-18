package com.ssafy.S14P21A205.game.season.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "season")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "season_id", nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeasonStatus status;

    @Column(name = "current_day")
    private Integer currentDay;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Season(SeasonStatus status, Integer currentDay, Integer totalDays, LocalDateTime startTime, LocalDateTime endTime) {
        this.status = status;
        this.currentDay = currentDay;
        this.totalDays = totalDays;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static Season createScheduled(int totalDays, LocalDateTime startTime, LocalDateTime endTime) {
        return new Season(SeasonStatus.SCHEDULED, 1, totalDays, startTime, endTime);
    }

    public void start() {
        this.status = SeasonStatus.IN_PROGRESS;
        if (currentDay == null || currentDay < 1) {
            this.currentDay = 1;
        }
    }

    public void finish() {
        this.status = SeasonStatus.FINISHED;
        if (totalDays != null) {
            this.currentDay = totalDays;
        }
    }

    public void syncCurrentDay(Integer currentDay) {
        if (currentDay == null) {
            return;
        }
        this.currentDay = currentDay;
    }

    public void updateEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}

