package com.ssafy.S14P21A205.action.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ActionLogId implements Serializable {

    private Long action;
    private Long store;
    private Integer gameDay;
}
