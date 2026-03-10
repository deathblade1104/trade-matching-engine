package com.shahbazsideprojects.tradematching.dto.order;

import com.shahbazsideprojects.tradematching.entity.enums.OrderStatus;
import com.shahbazsideprojects.tradematching.entity.enums.OrderStatusActor;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryResponseDto {

    private OrderStatus status;
    private OrderStatusActor actor;

    @JsonProperty("created_at")
    private String createdAt;
}
