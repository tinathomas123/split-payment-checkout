package com.splitpay.order.dto;

import com.splitpay.order.model.Order;
import com.splitpay.order.model.OrderStatus;
import java.math.BigDecimal;

public record OrderResponse(Long id, BigDecimal totalAmount,
                            String cardALast4, BigDecimal cardAAmount,
                            String cardBLast4, BigDecimal cardBAmount,
                            OrderStatus status) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getTotalAmount(),
                order.getCardALast4(), order.getCardAAmount(),
                order.getCardBLast4(), order.getCardBAmount(), order.getStatus());
    }
}
