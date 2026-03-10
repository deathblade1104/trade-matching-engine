package com.shahbazsideprojects.tradematching.engine;

import com.shahbazsideprojects.tradematching.entity.Order;
import com.shahbazsideprojects.tradematching.entity.OrderStatusHistory;
import com.shahbazsideprojects.tradematching.entity.Trade;
import com.shahbazsideprojects.tradematching.entity.enums.OrderSide;
import com.shahbazsideprojects.tradematching.entity.enums.OrderStatus;
import com.shahbazsideprojects.tradematching.entity.enums.OrderStatusActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs matching for one incoming order against the in-memory order book.
 *
 * <p>Algorithm: while the incoming order still has remaining quantity and the counter
 * side has liquidity at a matching price, we poll the best counter order, compute the
 * trade size, update both orders' remaining and status, record the trade and any
 * status changes. Partially filled counter orders are put back at the front of their
 * price level (addFirst) so they keep time priority.
 *
 * <p>All mutations are in-memory; the caller is responsible for persisting
 * MatchResult (orders, trades, statusLogs) and for adding the incoming order to the
 * book if it is still partial after matching.
 */
public class MatchingEngine {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngine.class);

    private final OrderBook book;

    public MatchingEngine(OrderBook book) {
        this.book = book;
    }

    /**
     * Match the incoming order against the counter side of the book.
     * Mutates the incoming order and any counter orders in place.
     *
     * @param incoming the order to match (not required to be in the book; e.g. a new order)
     * @return result with updated incoming order, all modified orders, trades, and status logs
     */
    public MatchResult matchOrder(Order incoming) {
        if (incoming.getRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            return MatchResult.builder()
                    .incomingOrder(incoming)
                    .ordersToUpdate(List.of(incoming))
                    .trades(List.of())
                    .statusLogs(List.of())
                    .build();
        }

        // Counter side: BUY matches against SELL book, SELL matches against BUY book.
        OrderSide counterSide = incoming.getSide() == OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY;
        List<Order> ordersToUpdate = new ArrayList<>();
        ordersToUpdate.add(incoming);
        List<Trade> trades = new ArrayList<>();
        List<OrderStatusHistory> statusLogs = new ArrayList<>();

        while (incoming.getRemaining().compareTo(BigDecimal.ZERO) > 0) {
            Order counterOrder = book.pollBest(counterSide);
            if (counterOrder == null) {
                break;
            }

            // Price compatibility: BUY can match SELL if bid >= ask; SELL can match BUY if ask <= bid.
            BigDecimal counterPrice = counterOrder.getPrice();
            if (!priceMatch(incoming, counterPrice)) {
                book.addFirst(counterOrder);
                break;
            }

            BigDecimal tradeQty = incoming.getRemaining().min(counterOrder.getRemaining());
            BigDecimal tradePrice = counterPrice; // Maker price (counter order's price).

            OrderStatus prevIncomingStatus = incoming.getStatus();
            OrderStatus prevCounterStatus = counterOrder.getStatus();

            incoming.setRemaining(incoming.getRemaining().subtract(tradeQty));
            counterOrder.setRemaining(counterOrder.getRemaining().subtract(tradeQty));
            incoming.setStatus(
                    incoming.getRemaining().compareTo(BigDecimal.ZERO) == 0 ? OrderStatus.FILLED : OrderStatus.PARTIAL);
            counterOrder.setStatus(
                    counterOrder.getRemaining().compareTo(BigDecimal.ZERO) == 0 ? OrderStatus.FILLED : OrderStatus.PARTIAL);

            ordersToUpdate.add(counterOrder);

            Long buyOrderId = incoming.getSide() == OrderSide.BUY ? incoming.getId() : counterOrder.getId();
            Long sellOrderId = incoming.getSide() == OrderSide.SELL ? incoming.getId() : counterOrder.getId();
            trades.add(Trade.builder()
                    .buyOrderId(buyOrderId)
                    .sellOrderId(sellOrderId)
                    .price(tradePrice)
                    .quantity(tradeQty)
                    .build());

            if (incoming.getStatus() != prevIncomingStatus) {
                statusLogs.add(OrderStatusHistory.builder()
                        .orderId(incoming.getId())
                        .status(incoming.getStatus())
                        .actor(OrderStatusActor.SYSTEM)
                        .build());
            }
            if (counterOrder.getStatus() != prevCounterStatus) {
                statusLogs.add(OrderStatusHistory.builder()
                        .orderId(counterOrder.getId())
                        .status(counterOrder.getStatus())
                        .actor(OrderStatusActor.SYSTEM)
                        .build());
            }

            if (counterOrder.getRemaining().compareTo(BigDecimal.ZERO) > 0) {
                book.addFirst(counterOrder);
            }

            log.debug("Trade executed: {} @ {} | Orders {} <-> {}",
                    tradeQty, tradePrice, incoming.getId(), counterOrder.getId());
        }

        return MatchResult.builder()
                .incomingOrder(incoming)
                .ordersToUpdate(ordersToUpdate)
                .trades(trades)
                .statusLogs(statusLogs)
                .build();
    }

    /**
     * True if the incoming order can match at the given counter price.
     * BUY matches when incoming.price >= counterPrice; SELL when incoming.price <= counterPrice.
     */
    private boolean priceMatch(Order incoming, BigDecimal counterPrice) {
        if (incoming.getSide() == OrderSide.BUY) {
            return incoming.getPrice().compareTo(counterPrice) >= 0;
        }
        return incoming.getPrice().compareTo(counterPrice) <= 0;
    }
}
