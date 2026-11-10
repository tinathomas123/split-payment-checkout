package com.splitpay.order.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank String productName,
        @Positive int quantity,
        @NotNull @Positive BigDecimal unitPrice) { }
