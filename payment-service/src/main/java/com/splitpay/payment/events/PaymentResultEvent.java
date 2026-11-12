package com.splitpay.payment.events;

/** Published to "payment.results" once both parts of the split payment have been processed. */
public record PaymentResultEvent(Long orderId, PaymentOutcome outcome, String reason) {

    public enum PaymentOutcome { COMPLETED, FAILED }
}
