package com.splitpay.payment.messaging;

import com.splitpay.payment.events.OrderPlacedEvent;
import com.splitpay.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedListener.class);

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String paymentResultsTopic;

    public OrderPlacedListener(PaymentService paymentService,
                               KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${app.topics.payment-results}") String paymentResultsTopic) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
        this.paymentResultsTopic = paymentResultsTopic;
    }

    @KafkaListener(topics = "${app.topics.orders-placed}")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlaced for order {}", event.orderId());
        // Any exception thrown here is handled by the DefaultErrorHandler: retried, then sent to the DLT
        paymentService.processSplitPayment(event).ifPresent(result -> {
            kafkaTemplate.send(paymentResultsTopic, String.valueOf(result.orderId()), result);
            log.info("Published {} for order {}", result.outcome(), result.orderId());
        });
    }
}
