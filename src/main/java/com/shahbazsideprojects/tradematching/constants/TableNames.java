package com.shahbazsideprojects.tradematching.constants;

/**
 * Centralized table names for JPA entities. Use with @Table(name = TableNames.USERS) etc.
 */
public enum TableNames {
    USERS("users"),
    ORDERS("orders"),
    TRADES("trades"),
    ORDER_STATUS_HISTORY("order_status_history");

    private final String name;

    TableNames(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
