package com.sideprojects.tradematching.scheduler;

import com.sideprojects.tradematching.entity.Order;
import com.sideprojects.tradematching.entity.enums.OrderStatus;
import com.sideprojects.tradematching.kafka.OrderEventProducer;
import com.sideprojects.tradematching.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryScheduler.class);

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    @Scheduled(cron = "0 0 * * * *") // every hour
    public void scheduleExpiredOrders() {
        List<Order> openOrPartial = orderRepository.findByStatusIn(List.of(OrderStatus.OPEN, OrderStatus.PARTIAL));
        Instant now = Instant.now();
        int sent = 0;
        for (Order order : openOrPartial) {
            if (order.getValidityDays() == null || order.getValidityDays() <= 0) continue;
            Instant expiryAt = order.getCreatedAt().plusSeconds(order.getValidityDays() * 86400L);
            if (expiryAt.isBefore(now)) {
                orderEventProducer.sendExpireOrder(order.getId());
                sent++;
            }
        }
        if (sent > 0) {
            log.info("Scheduled {} orders for expiry", sent);
        }
    }
}
