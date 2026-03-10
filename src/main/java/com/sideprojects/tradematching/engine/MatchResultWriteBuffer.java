package com.sideprojects.tradematching.engine;

import com.sideprojects.tradematching.config.AppProperties;
import com.sideprojects.tradematching.entity.Order;
import com.sideprojects.tradematching.entity.OrderStatusHistory;
import com.sideprojects.tradematching.entity.Trade;
import com.sideprojects.tradematching.repository.OrderRepository;
import com.sideprojects.tradematching.repository.OrderStatusHistoryRepository;
import com.sideprojects.tradematching.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Buffers match results and flushes them to the DB in batches to control write throughput.
 *
 * <p>When {@code app.matching.persistence.flush-interval-ms > 0}, the scheduler calls
 * {@link #flush()} periodically. Multiple match results are merged (last update per order wins)
 * and persisted in one transaction, reducing DB round-trips and smoothing write load.
 *
 * <p>When {@code flush-interval-ms == 0}, the matching service calls {@link #flush()} after
 * each {@link #enqueue(MatchResult)}, so every match is persisted immediately (no batching).
 */
@Component
@RequiredArgsConstructor
public class MatchResultWriteBuffer {

    private static final Logger log = LoggerFactory.getLogger(MatchResultWriteBuffer.class);

    private final AppProperties appProperties;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    private final BlockingQueue<MatchResult> queue = new LinkedBlockingQueue<>();

    /**
     * Add a match result to the buffer. Does not block. When batching is disabled
     * (flush-interval-ms == 0), the caller should call {@link #flush()} immediately after.
     */
    public void enqueue(MatchResult result) {
        queue.offer(result);
    }

    /**
     * Drain up to {@code batchSize} results from the queue, merge orders by id (last wins),
     * and persist in one transaction. Safe to call from a scheduler or after each match.
     */
    @Transactional
    public void flush() {
        int batchSize = appProperties.getMatching().getPersistence().getBatchSize();
        List<MatchResult> batch = new ArrayList<>();
        queue.drainTo(batch, batchSize);
        if (batch.isEmpty()) {
            return;
        }

        // Merge orders: same order may appear in multiple results (e.g. partial fills);
        // keep the last occurrence so we persist final state.
        Map<Long, Order> ordersById = new LinkedHashMap<>();
        List<Trade> allTrades = new ArrayList<>();
        List<OrderStatusHistory> allLogs = new ArrayList<>();

        for (MatchResult result : batch) {
            for (Order order : result.getOrdersToUpdate()) {
                ordersById.put(order.getId(), order);
            }
            allTrades.addAll(result.getTrades());
            allLogs.addAll(result.getStatusLogs());
        }

        if (!ordersById.isEmpty()) {
            orderRepository.saveAll(new ArrayList<>(ordersById.values()));
        }
        if (!allTrades.isEmpty()) {
            tradeRepository.saveAll(allTrades);
        }
        if (!allLogs.isEmpty()) {
            orderStatusHistoryRepository.saveAll(allLogs);
        }

        log.debug("Flushed {} match results ({} orders, {} trades) to DB",
                batch.size(), ordersById.size(), allTrades.size());
    }

    /** Current number of pending results in the buffer (for metrics/debugging). */
    public int pendingCount() {
        return queue.size();
    }
}
