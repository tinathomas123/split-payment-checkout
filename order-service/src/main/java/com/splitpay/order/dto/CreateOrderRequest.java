package com.splitpay.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/** Request body for POST /orders: the cart items plus how the total is split across two cards. */
public record CreateOrderRequest(
        @NotEmpty List<@Valid OrderItemRequest> items,
        @NotNull @Positive BigDecimal cardAAmount,
        @NotNull @Positive BigDecimal cardBAmount) { }
