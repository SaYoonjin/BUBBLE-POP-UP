package com.ssafy.S14P21A205.game.environment.entity;

import com.ssafy.S14P21A205.store.entity.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "traffic")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Traffic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "traffic_id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(name = "traffic_status", nullable = false)
    private TrafficStatus trafficStatus;

    @Column(name = "source_batch_key", length = 64)
    private String sourceBatchKey;

    public static Traffic create(Location location, LocalDateTime date, TrafficStatus trafficStatus) {
        return create(location, date, trafficStatus, null);
    }

    public static Traffic create(Location location, LocalDateTime date, TrafficStatus trafficStatus, String sourceBatchKey) {
        Traffic traffic = new Traffic();
        traffic.location = location;
        traffic.date = date;
        traffic.trafficStatus = trafficStatus;
        traffic.sourceBatchKey = sourceBatchKey;
        return traffic;
    }
}
