package com.ssafy.S14P21A205.action.entity;

import com.ssafy.S14P21A205.store.entity.Store;
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
@Table(name = "action_log")
@IdClass(ActionLogId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionLog {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "game_day", nullable = false)
    private Integer gameDay;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed;

    public ActionLog(Action action, Store store, Integer gameDay) {
        this.action = action;
        this.store = store;
        this.gameDay = gameDay;
        this.isUsed = true;
    }
}
