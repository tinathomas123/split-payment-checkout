package com.splitpay.payment.model;

public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    REFUNDED   // card A refunded after card B was declined
}
