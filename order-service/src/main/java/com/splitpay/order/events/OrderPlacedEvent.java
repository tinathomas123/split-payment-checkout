package com.splitpay.order.events;

import java.math.BigDecimal;

/** Published to "orders.placed" when an order is created. */
public record OrderPlacedEvent(
        Long orderId,
        BigDecimal orderTotal,
        String cardALast4,
        BigDecimal cardAAmount,
        String cardBLast4,
        BigDecimal cardBAmount) { }
