package com.splitpay.order.dto;

import com.splitpay.order.model.Order;
import com.splitpay.order.model.OrderStatus;
import java.math.BigDecimal;

public record OrderResponse(Long id, BigDecimal totalAmount, BigDecimal cardAAmount,
                            BigDecimal cardBAmount, OrderStatus status) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getTotalAmount(),
                order.getCardAAmount(), order.getCardBAmount(), order.getStatus());
    }
}
