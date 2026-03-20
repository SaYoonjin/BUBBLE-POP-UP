package com.ssafy.S14P21A205.store.entity;

import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "store")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_location_id")
    private Location pendingLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "store_name", nullable = false, length = 50)
    private String storeName;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "playable_from_day")
    private Integer playableFromDay;

    @Column(name = "purchase_seed")
    private Long purchaseSeed;

    @Column(name = "purchase_cursor")
    private Integer purchaseCursor = 0;

    @Column(name = "pending_location_apply_day")
    private Integer pendingLocationApplyDay;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Store(
            User user,
            Location location,
            Menu menu,
            Season season,
            String storeName,
            Integer price,
            Integer playableFromDay
    ) {
        this.user = user;
        this.location = location;
        this.menu = menu;
        this.season = season;
        this.storeName = storeName;
        this.price = price;
        this.playableFromDay = playableFromDay;
    }

    public static Store create(
            User user,
            Location location,
            Menu menu,
            Season season,
            String storeName,
            Integer price,
            Integer playableFromDay
    ) {
        return new Store(user, location, menu, season, storeName, price, playableFromDay);
    }

    public void changeLocation(Location location) {
        this.location = location;
    }

    public void reserveLocationChange(Location location, Integer applyDay) {
        this.pendingLocation = location;
        this.pendingLocationApplyDay = applyDay;
    }

    public void applyPendingLocationChange() {
        if (pendingLocation == null) {
            return;
        }
        this.location = pendingLocation;
    }

    public void changeMenu(Menu menu) {
        this.menu = menu;
    }

    public void changePrice(Integer price) {
        this.price = price;
    }

    public void initializePurchaseQueue(long purchaseSeed) {
        this.purchaseSeed = purchaseSeed;
        this.purchaseCursor = 0;
    }

    public void changePurchaseCursor(Integer purchaseCursor) {
        this.purchaseCursor = purchaseCursor == null ? 0 : purchaseCursor;
    }
}

