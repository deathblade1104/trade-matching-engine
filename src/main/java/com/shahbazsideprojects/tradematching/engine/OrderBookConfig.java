package com.shahbazsideprojects.tradematching.engine;

import com.shahbazsideprojects.tradematching.entity.Order;
import com.shahbazsideprojects.tradematching.entity.enums.OrderStatus;

import java.math.BigDecimal;
import com.shahbazsideprojects.tradematching.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Provides the in-memory order book and matching engine as Spring beans.
 * On startup, loads all OPEN/PARTIAL orders with remaining > 0 from the DB into the book
 * so that matching runs against memory instead of repeated DB queries.
 */
@Configuration
@RequiredArgsConstructor
public class OrderBookConfig {

    private static final Logger log = LoggerFactory.getLogger(OrderBookConfig.class);

    private final OrderRepository orderRepository;
    private final OrderBook orderBook;

    @Bean
    public OrderBook orderBook() {
        return new OrderBook();
    }

    @Bean
    public MatchingEngine matchingEngine(OrderBook orderBook) {
        return new MatchingEngine(orderBook);
    }

    /**
     * After the application context is ready, load all active orders into the book.
     * This runs once per instance; the book then stays in sync via add/remove when
     * we process new orders and when we persist match results.
     */
    @PostConstruct
    public void loadBookOnStartup() {
        List<OrderStatus> active = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);
        List<Order> orders = orderRepository.findActiveOrdersForBook(active, BigDecimal.ZERO);
        for (Order order : orders) {
            orderBook.add(order);
        }
        log.info("Order book loaded with {} active orders", orderBook.size());
    }
}
