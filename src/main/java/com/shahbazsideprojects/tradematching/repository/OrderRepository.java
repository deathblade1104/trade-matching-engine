package com.shahbazsideprojects.tradematching.repository;

import com.shahbazsideprojects.tradematching.entity.Order;
import com.shahbazsideprojects.tradematching.entity.enums.OrderSide;
import com.shahbazsideprojects.tradematching.entity.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findBySideAndStatusInOrderByPriceAscCreatedAtAsc(
            OrderSide side, List<OrderStatus> statuses, Pageable pageable);

    List<Order> findBySideAndStatusInOrderByPriceDescCreatedAtAsc(
            OrderSide side, List<OrderStatus> statuses, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    long countByUserId(Long userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.side = :side AND o.status IN :statuses ORDER BY o.price DESC, o.createdAt ASC")
    List<Order> findActiveBySideWithUserOrderByPriceDesc(@Param("side") OrderSide side, @Param("statuses") List<OrderStatus> statuses, Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.side = :side AND o.status IN :statuses ORDER BY o.price ASC, o.createdAt ASC")
    List<Order> findActiveBySideWithUserOrderByPriceAsc(@Param("side") OrderSide side, @Param("statuses") List<OrderStatus> statuses, Pageable pageable);

    List<Order> findByStatusIn(List<OrderStatus> statuses);

    /**
     * All OPEN/PARTIAL orders with remaining > 0, for loading the in-memory order book at startup.
     * No pagination; used once to hydrate the book.
     */
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses AND o.remaining > :zero")
    List<Order> findActiveOrdersForBook(@Param("statuses") List<OrderStatus> statuses, @Param("zero") BigDecimal zero);
}
