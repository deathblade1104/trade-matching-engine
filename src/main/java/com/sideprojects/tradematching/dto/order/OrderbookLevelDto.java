package com.sideprojects.tradematching.dto.order;

import com.sideprojects.tradematching.entity.enums.OrderSide;
import com.sideprojects.tradematching.entity.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderbookLevelDto {

    private String price;
    private BigDecimal remaining;

    @JsonProperty("user_name")
    private String userName;

    private OrderStatus status;
    private OrderSide side;

    @JsonProperty("created_at")
    private String createdAt;
}
