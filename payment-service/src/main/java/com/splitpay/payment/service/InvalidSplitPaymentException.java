package com.splitpay.payment.service;

public class InvalidSplitPaymentException extends RuntimeException {
    public InvalidSplitPaymentException(String message) {
        super(message);
    }
}
