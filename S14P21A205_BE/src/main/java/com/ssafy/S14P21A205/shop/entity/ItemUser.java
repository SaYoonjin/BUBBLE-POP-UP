package com.ssafy.S14P21A205.shop.entity;

import com.ssafy.S14P21A205.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "item_user")
@IdClass(ItemUserId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemUser {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_purchased", nullable = false)
    private Boolean isPurchased;

    private ItemUser(Item item, User user, Boolean isPurchased) {
        this.item = item;
        this.user = user;
        this.isPurchased = isPurchased;
    }

    public static ItemUser purchase(Item item, User user) {
        return new ItemUser(item, user, true);
    }

    public void markPurchased() {
        this.isPurchased = true;
    }
}
