package com.splitpay.payment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Day 1: posted manually to test the table.
 * Day 2: this data will arrive in an OrderPlaced Kafka event instead.
 */
public record SplitPaymentRequest(
        @NotNull Long orderId,
        @NotNull @Positive BigDecimal orderTotal,
        @NotBlank @Pattern(regexp = "\\d{4}") String cardALast4,
        @NotNull @Positive BigDecimal cardAAmount,
        @NotBlank @Pattern(regexp = "\\d{4}") String cardBLast4,
        @NotNull @Positive BigDecimal cardBAmount) { }
