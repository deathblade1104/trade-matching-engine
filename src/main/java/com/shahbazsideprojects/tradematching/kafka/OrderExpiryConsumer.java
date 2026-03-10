package com.shahbazsideprojects.tradematching.kafka;

import com.shahbazsideprojects.tradematching.entity.Order;
import com.shahbazsideprojects.tradematching.entity.OrderStatusHistory;
import com.shahbazsideprojects.tradematching.entity.enums.OrderStatus;
import com.shahbazsideprojects.tradematching.entity.enums.OrderStatusActor;
import com.shahbazsideprojects.tradematching.repository.OrderRepository;
import com.shahbazsideprojects.tradematching.repository.OrderStatusHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderExpiryConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryConsumer.class);

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @KafkaListener(topics = OrderEventProducer.TOPIC_EXPIRE_ORDER, groupId = "order-expiry-group")
    @Transactional
    public void consumeExpireOrder(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("orderId")) {
            log.warn("No orderId in expire-order payload");
            return;
        }
        Long orderId = ((Number) payload.get("orderId")).longValue();
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order {} not found for expiry", orderId);
            return;
        }
        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.EXPIRED) {
            return;
        }
        order.setStatus(OrderStatus.EXPIRED);
        orderRepository.save(order);
        OrderStatusHistory h = OrderStatusHistory.builder()
                .orderId(order.getId())
                .status(OrderStatus.EXPIRED)
                .actor(OrderStatusActor.SYSTEM)
                .build();
        orderStatusHistoryRepository.save(h);
        log.debug("Order {} expired", orderId);
    }
}
