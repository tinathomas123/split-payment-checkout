package com.splitpay.payment.events;

import java.math.BigDecimal;

/** Consumed from "orders.placed". Mirrors order-service's event, but each service owns its own copy. */
public record OrderPlacedEvent(
        Long orderId,
        BigDecimal orderTotal,
        String cardALast4,
        BigDecimal cardAAmount,
        String cardBLast4,
        BigDecimal cardBAmount) { }
