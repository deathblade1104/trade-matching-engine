package com.sideprojects.tradematching.entity;

import com.sideprojects.tradematching.constants.TableNames;
import com.sideprojects.tradematching.entity.enums.OrderStatus;
import com.sideprojects.tradematching.entity.enums.OrderStatusActor;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = TableNames.ORDER_STATUS_HISTORY)
public class OrderStatusHistory extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderStatusActor actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;
}
