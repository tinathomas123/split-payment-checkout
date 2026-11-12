package com.splitpay.order.messaging;

import com.splitpay.order.events.PaymentResultEvent;
import com.splitpay.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final OrderService orderService;

    public PaymentResultListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "${app.topics.payment-results}")
    public void onPaymentResult(PaymentResultEvent event) {
        log.info("Payment result for order {}: {} ({})", event.orderId(), event.outcome(), event.reason());
        orderService.applyPaymentResult(event);
    }
}
