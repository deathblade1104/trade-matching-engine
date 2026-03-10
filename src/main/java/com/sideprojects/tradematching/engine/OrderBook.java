package com.sideprojects.tradematching.engine;

import com.sideprojects.tradematching.entity.Order;
import com.sideprojects.tradematching.entity.enums.OrderSide;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * In-memory order book with price-time priority per side.
 *
 * <ul>
 *   <li><b>BUY book</b>: best bid = highest price first (TreeMap with reverse order).</li>
 *   <li><b>SELL book</b>: best ask = lowest price first (TreeMap natural order).</li>
 *   <li>At each price level, orders are FIFO (Deque).</li>
 * </ul>
 *
 * <p>Not thread-safe. Use a single writer (e.g. one Kafka consumer thread) or external
 * synchronization (e.g. {@code synchronized (orderBook) { ... } }) when matching.
 */
public class OrderBook {

    /**
     * BUY side: descending price so firstKey() = best bid (highest price).
     * Key = price, value = queue of orders at that price (FIFO).
     */
    private final NavigableMap<BigDecimal, Deque<Order>> buyBook =
            new TreeMap<>(Comparator.reverseOrder());

    /**
     * SELL side: ascending price so firstKey() = best ask (lowest price).
     */
    private final NavigableMap<BigDecimal, Deque<Order>> sellBook = new TreeMap<>();

    /**
     * Quick lookup by order id: used for cancel/expiry and for "get and remove" when
     * we need to take an order out of the book to match it as incoming (e.g. on re-process).
     */
    private final Map<Long, Order> ordersInBook = new HashMap<>();

    private NavigableMap<BigDecimal, Deque<Order>> bookFor(OrderSide side) {
        return side == OrderSide.BUY ? buyBook : sellBook;
    }

    /**
     * Add a resting order to the book. Only adds if remaining > 0.
     * Puts it at the back of its price level (FIFO).
     */
    public void add(Order order) {
        if (order.getRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal price = order.getPrice();
        bookFor(order.getSide())
                .computeIfAbsent(price, p -> new ArrayDeque<>())
                .addLast(order);
        ordersInBook.put(order.getId(), order);
    }

    /**
     * Remove an order by id (e.g. cancel or expiry). No-op if not in book.
     */
    public void remove(Long orderId) {
        Order order = ordersInBook.remove(orderId);
        if (order == null) {
            return;
        }
        Deque<Order> level = bookFor(order.getSide()).get(order.getPrice());
        if (level != null) {
            level.remove(order);
            if (level.isEmpty()) {
                bookFor(order.getSide()).remove(order.getPrice());
            }
        }
    }

    /**
     * Remove and return an order by id. Used when we need to take a resting order
     * out of the book to match it again as "incoming" (e.g. after a re-process event).
     * Returns null if the order was not in the book (e.g. first-time process).
     */
    public Order getAndRemove(Long orderId) {
        Order order = ordersInBook.remove(orderId);
        if (order == null) {
            return null;
        }
        Deque<Order> level = bookFor(order.getSide()).get(order.getPrice());
        if (level != null) {
            level.remove(order);
            if (level.isEmpty()) {
                bookFor(order.getSide()).remove(order.getPrice());
            }
        }
        return order;
    }

    /** Best bid price, or null if no buy orders. */
    public BigDecimal bestBid() {
        return buyBook.isEmpty() ? null : buyBook.firstKey();
    }

    /** Best ask price, or null if no sell orders. */
    public BigDecimal bestAsk() {
        return sellBook.isEmpty() ? null : sellBook.firstKey();
    }

    /**
     * Remove and return the first order at the best level for the given side.
     * Used by the matching engine to consume liquidity. Removes the price level if empty.
     * Returns null if the book for that side is empty.
     */
    public Order pollBest(OrderSide side) {
        NavigableMap<BigDecimal, Deque<Order>> book = bookFor(side);
        if (book.isEmpty()) {
            return null;
        }
        BigDecimal best = book.firstKey();
        Deque<Order> level = book.get(best);
        Order order = level.pollFirst();
        if (level.isEmpty()) {
            book.remove(best);
        }
        if (order != null) {
            ordersInBook.remove(order.getId());
        }
        return order;
    }

    /**
     * Put a partially filled counter order back at the front of its price level
     * so it keeps time priority for the next match.
     */
    public void addFirst(Order order) {
        if (order.getRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal price = order.getPrice();
        bookFor(order.getSide())
                .computeIfAbsent(price, p -> new ArrayDeque<>())
                .addFirst(order);
        ordersInBook.put(order.getId(), order);
    }

    /** Number of orders currently in the book (for metrics/debugging). */
    public int size() {
        return ordersInBook.size();
    }
}
