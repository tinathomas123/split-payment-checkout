package com.splitpay.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for POST /orders: the cart items plus how the total is split across two cards.
 * Only the last 4 digits of each card are sent (payments are simulated).
 * Test cards: last 4 "0002" is declined; "9999" simulates the payment provider being down.
 */
public record CreateOrderRequest(
        @NotEmpty List<@Valid OrderItemRequest> items,
        @NotBlank @Pattern(regexp = "\\d{4}") String cardALast4,
        @NotNull @Positive BigDecimal cardAAmount,
        @NotBlank @Pattern(regexp = "\\d{4}") String cardBLast4,
        @NotNull @Positive BigDecimal cardBAmount) { }
