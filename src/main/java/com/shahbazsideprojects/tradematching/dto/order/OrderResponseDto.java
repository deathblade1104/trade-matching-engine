package com.shahbazsideprojects.tradematching.dto.order;

import com.shahbazsideprojects.tradematching.entity.enums.OrderSide;
import com.shahbazsideprojects.tradematching.entity.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {

    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    private OrderSide side;
    private String price;
    private String quantity;
    private String remaining;
    private OrderStatus status;

    @JsonProperty("validity_days")
    private Integer validityDays;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("status_history")
    private List<OrderStatusHistoryResponseDto> statusHistory;
}
