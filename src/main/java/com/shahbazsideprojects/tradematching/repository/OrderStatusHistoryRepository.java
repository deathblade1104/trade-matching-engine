package com.shahbazsideprojects.tradematching.repository;

import com.shahbazsideprojects.tradematching.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
