package com.splitpay.order.service;

public class InvalidSplitPaymentException extends RuntimeException {
    public InvalidSplitPaymentException(String message) {
        super(message);
    }
}
