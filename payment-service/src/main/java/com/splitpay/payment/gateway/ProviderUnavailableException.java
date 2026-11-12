package com.splitpay.payment.gateway;

/** A temporary technical failure (e.g. provider timeout). Worth retrying, unlike a card decline. */
public class ProviderUnavailableException extends RuntimeException {
    public ProviderUnavailableException(String message) {
        super(message);
    }
}
