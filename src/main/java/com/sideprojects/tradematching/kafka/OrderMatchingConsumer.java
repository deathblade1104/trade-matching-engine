package com.sideprojects.tradematching.kafka;

import com.sideprojects.tradematching.entity.Order;
import com.sideprojects.tradematching.entity.enums.OrderStatus;
import com.sideprojects.tradematching.service.OrderMatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Consumes "process order" events from Kafka. Delegates to OrderMatchingService which uses
 * the in-memory order book and matching engine (single-threaded matching, then persist).
 * Re-enqueues the same order if it is partially filled so the next run can continue matching.
 */
@Component
@RequiredArgsConstructor
public class OrderMatchingConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderMatchingConsumer.class);
    private static final int MAX_REPROCESS_COUNT = 10;

    private final OrderMatchingService orderMatchingService;
    private final OrderEventProducer orderEventProducer;

    @KafkaListener(topics = OrderEventProducer.TOPIC_PROCESS_ORDER, groupId = "order-matching-group")
    public void consumeProcessOrder(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("orderId")) {
            log.warn("No orderId in process-order payload");
            return;
        }
        Long orderId = ((Number) payload.get("orderId")).longValue();
        int count = payload.containsKey("reprocessCount") ? ((Number) payload.get("reprocessCount")).intValue() : 0;

        if (count >= MAX_REPROCESS_COUNT) {
            log.error("Max reprocess count reached for order id: {}. No further processing.", orderId);
            return;
        }

        try {
            Order updated = orderMatchingService.matchOrder(orderId);
            if (updated.getStatus() == OrderStatus.FILLED) {
                log.info("Order {} fully matched.", orderId);
                return;
            }
            if (updated.getRemaining().compareTo(java.math.BigDecimal.ZERO) > 0) {
                log.info("Order {} partially filled, re-enqueuing...", orderId);
                orderEventProducer.sendProcessOrder(orderId, count + 1);
            }
        } catch (Exception e) {
            log.error("Error processing order {}: {}", orderId, e.getMessage(), e);
        }
    }
}
