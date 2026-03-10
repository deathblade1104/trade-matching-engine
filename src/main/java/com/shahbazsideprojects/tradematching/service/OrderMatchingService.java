package com.shahbazsideprojects.tradematching.service;

import com.shahbazsideprojects.tradematching.config.AppProperties;
import com.shahbazsideprojects.tradematching.engine.MatchResult;
import com.shahbazsideprojects.tradematching.engine.MatchResultWriteBuffer;
import com.shahbazsideprojects.tradematching.engine.MatchingEngine;
import com.shahbazsideprojects.tradematching.engine.OrderBook;
import com.shahbazsideprojects.tradematching.entity.Order;
import com.shahbazsideprojects.tradematching.entity.enums.OrderStatus;
import com.shahbazsideprojects.tradematching.exception.ResourceNotFoundException;
import com.shahbazsideprojects.tradematching.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Runs order matching using the in-memory order book and matching engine.
 *
 * <p><b>Performance</b>: Matching is done entirely in memory (no repeated DB pagination).
 * We synchronize on the order book so only one thread runs matching at a time. Writes go
 * through {@link MatchResultWriteBuffer}: when batching is disabled, each match is persisted
 * immediately; when batching is enabled, results are flushed on a schedule to control DB throughput.
 *
 * <p><b>Flow</b>:
 * <ol>
 *   <li>Get the order: if it was re-enqueued (partial fill), it may already be in the book —
 *       we getAndRemove it so we can match it again as "incoming". Otherwise load from DB.</li>
 *   <li>Run the matching engine (in-memory) to produce trades and updates.</li>
 *   <li>Enqueue the result to the write buffer; flush immediately if batching is off.</li>
 *   <li>If the incoming order is still partial, add it back to the book (and the consumer will re-enqueue).</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class OrderMatchingService {

    private static final Logger log = LoggerFactory.getLogger(OrderMatchingService.class);

    private final OrderBook orderBook;
    private final MatchingEngine matchingEngine;
    private final MatchResultWriteBuffer writeBuffer;
    private final AppProperties appProperties;
    private final OrderRepository orderRepository;

    /**
     * Match the order by id: load or take from book, run engine, persist, optionally re-add to book.
     * Caller (e.g. Kafka consumer) should re-enqueue if the returned order is still partial.
     */
    public Order matchOrder(Long orderId) {
        log.debug("Matching order [id={}]", orderId);

        // If this order was previously partially filled, it may be resting in the book.
        // Take it out so we can match it again as the "incoming" order.
        Order order = orderBook.getAndRemove(orderId);
        if (order == null) {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));
        }

        if (order.getStatus() == OrderStatus.FILLED || order.getRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Order {} already filled or no remaining, skipping", order.getId());
            return order;
        }

        // Single-threaded matching: only one thread at a time can run the engine and update the book.
        MatchResult result;
        synchronized (orderBook) {
            result = matchingEngine.matchOrder(order);
        }

        // Send to write buffer; when batching is disabled, flush now so DB is updated immediately.
        writeBuffer.enqueue(result);
        if (!appProperties.getMatching().getPersistence().isBatchingEnabled()) {
            writeBuffer.flush();
        }

        // If the incoming order is still partial, add it back to the book so future matches can fill it.
        if (result.getIncomingOrder().getRemaining().compareTo(BigDecimal.ZERO) > 0) {
            orderBook.add(result.getIncomingOrder());
        }

        return result.getIncomingOrder();
    }
}
