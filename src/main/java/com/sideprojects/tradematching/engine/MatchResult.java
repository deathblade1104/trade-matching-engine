package com.sideprojects.tradematching.engine;

import com.sideprojects.tradematching.entity.Order;
import com.sideprojects.tradematching.entity.OrderStatusHistory;
import com.sideprojects.tradematching.entity.Trade;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Result of running the matching engine on one incoming order.
 * Contains the updated incoming order and all side effects to persist in one transaction:
 * - ordersToUpdate: incoming + any counter orders that were partially or fully filled
 * - trades: all trades produced
 * - statusLogs: status history entries for any order that changed status (PARTIAL/FILLED)
 */
@Value
@Builder
public class MatchResult {

    /** The incoming order after matching (remaining and status updated in place). */
    Order incomingOrder;

    /** All orders that were modified (incoming + counter orders); save these to DB. */
    List<Order> ordersToUpdate;

    /** Trades to insert. */
    List<Trade> trades;

    /** Order status history rows to insert (actor = SYSTEM). */
    List<OrderStatusHistory> statusLogs;
}
