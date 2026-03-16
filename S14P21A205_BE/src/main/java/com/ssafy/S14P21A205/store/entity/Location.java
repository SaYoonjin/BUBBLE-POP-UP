package com.ssafy.S14P21A205.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "location")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "location_name", nullable = false, length = 50)
    private String locationName;

    @Column(nullable = false)
    private Integer rent;

    @Column(name = "interior_cost", nullable = false)
    private Integer interiorCost;
}
