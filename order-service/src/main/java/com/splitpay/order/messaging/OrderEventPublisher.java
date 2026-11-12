package com.splitpay.order.messaging;

import com.splitpay.order.events.OrderPlacedEvent;
import com.splitpay.order.model.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String ordersPlacedTopic;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${app.topics.orders-placed}") String ordersPlacedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.ordersPlacedTopic = ordersPlacedTopic;
    }

    public void publishOrderPlaced(Order order) {
        OrderPlacedEvent event = new OrderPlacedEvent(order.getId(), order.getTotalAmount(),
                order.getCardALast4(), order.getCardAAmount(),
                order.getCardBLast4(), order.getCardBAmount());

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // Send only after the order is committed to the database, so payment-service never
            // processes (and order-service never receives a result for) an order that isn't saved.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
        } else {
            send(event);
        }
    }

    private void send(OrderPlacedEvent event) {
        // The order ID is the message key, so all events for one order go to the same partition, in order.
        kafkaTemplate.send(ordersPlacedTopic, String.valueOf(event.orderId()), event);
    }
}
