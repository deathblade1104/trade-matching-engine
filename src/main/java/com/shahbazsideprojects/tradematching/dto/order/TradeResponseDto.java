package com.shahbazsideprojects.tradematching.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponseDto {

    private Long id;
    private String price;
    private String quantity;

    @JsonProperty("buy_order_id")
    private Long buyOrderId;

    @JsonProperty("sell_order_id")
    private Long sellOrderId;

    @JsonProperty("created_at")
    private String createdAt;
}
