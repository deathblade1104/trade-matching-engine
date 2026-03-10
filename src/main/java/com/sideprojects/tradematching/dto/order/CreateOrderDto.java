package com.sideprojects.tradematching.dto.order;

import com.sideprojects.tradematching.entity.enums.OrderSide;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderDto {

    @NotNull
    private OrderSide side;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal price;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal quantity;

    @JsonProperty("validity_days")
    @Min(1)
    @Max(60)
    private Integer validityDays = 60;
}
