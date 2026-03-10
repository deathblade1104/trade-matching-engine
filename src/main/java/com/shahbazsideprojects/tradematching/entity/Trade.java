package com.shahbazsideprojects.tradematching.entity;

import com.shahbazsideprojects.tradematching.constants.TableNames;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = TableNames.TRADES.getName())
public class Trade extends BaseEntity {

    @Column(name = "buy_order_id", nullable = false)
    private Long buyOrderId;

    @Column(name = "sell_order_id", nullable = false)
    private Long sellOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buy_order_id", insertable = false, updatable = false)
    private Order buyOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sell_order_id", insertable = false, updatable = false)
    private Order sellOrder;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;
}
