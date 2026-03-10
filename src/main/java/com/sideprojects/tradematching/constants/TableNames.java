package com.sideprojects.tradematching.constants;

/**
 * Centralized table names for JPA entities. Use with @Table(name = TableNames.USERS) etc.
 * Must be compile-time constants for use in annotations.
 */
public final class TableNames {

    public static final String USERS = "users";
    public static final String ORDERS = "orders";
    public static final String TRADES = "trades";
    public static final String ORDER_STATUS_HISTORY = "order_status_history";

    private TableNames() {}
}
