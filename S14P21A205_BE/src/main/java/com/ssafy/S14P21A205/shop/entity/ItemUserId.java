package com.ssafy.S14P21A205.shop.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ItemUserId implements Serializable {

    private Long item;
    private Integer user;
}
