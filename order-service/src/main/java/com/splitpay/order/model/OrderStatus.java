package com.splitpay.order.model;

public enum OrderStatus {
    PENDING_PAYMENT,   // order created, waiting for both payments (Day 2: set by Kafka events)
    PAID,
    PAYMENT_FAILED
}
