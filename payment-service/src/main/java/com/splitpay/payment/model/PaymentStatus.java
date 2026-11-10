package com.splitpay.payment.model;

public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    REFUNDED   // used in the Step Functions saga on Day 5
}
