package com.shahbazsideprojects.tradematching.entity;

import com.shahbazsideprojects.tradematching.constants.TableNames;
import com.shahbazsideprojects.tradematching.entity.enums.OrderSide;
import com.shahbazsideprojects.tradematching.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = TableNames.ORDERS.getName(), indexes = {
        @Index(name = "idx_side_status_price_createdat", columnList = "side, status, price, created_at"),
        @Index(name = "idx_user_id", columnList = "user_id")
})
public class Order extends BaseWithUpdatedBy {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private OrderSide side;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal remaining;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderStatus status = OrderStatus.OPEN;

    @Column(name = "validity_days", nullable = false)
    private Integer validityDays = 60;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();
}
