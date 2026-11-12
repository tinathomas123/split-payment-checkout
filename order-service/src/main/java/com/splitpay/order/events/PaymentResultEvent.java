package com.splitpay.order.events;

/** Consumed from "payment.results": the outcome of the split payment for one order. */
public record PaymentResultEvent(Long orderId, PaymentOutcome outcome, String reason) {

    public enum PaymentOutcome { COMPLETED, FAILED }
}
