package com.splitpay.payment.dto;

import com.splitpay.payment.model.Payment;
import com.splitpay.payment.model.PaymentStatus;
import java.math.BigDecimal;

public record PaymentResponse(Long id, Long orderId, int splitPart, String cardLast4,
                              BigDecimal amount, PaymentStatus status) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getSplitPart(),
                p.getCardLast4(), p.getAmount(), p.getStatus());
    }
}
