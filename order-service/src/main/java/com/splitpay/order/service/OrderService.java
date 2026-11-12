package com.splitpay.order.service;

import com.splitpay.order.dto.CreateOrderRequest;
import com.splitpay.order.events.PaymentResultEvent;
import com.splitpay.order.messaging.OrderEventPublisher;
import com.splitpay.order.model.Order;
import com.splitpay.order.model.OrderItem;
import com.splitpay.order.model.OrderStatus;
import com.splitpay.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        List<OrderItem> items = request.items().stream()
                .map(i -> new OrderItem(i.productName(), i.quantity(), i.unitPrice()))
                .toList();

        BigDecimal total = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        validateSplit(total, request.cardAAmount(), request.cardBAmount());

        Order saved = orderRepository.save(new Order(items, total,
                request.cardALast4(), request.cardAAmount(),
                request.cardBLast4(), request.cardBAmount()));

        // The publisher waits until this transaction commits before sending. If the app crashed
        // between the commit and the send, the event would be lost; a production system would use
        // the transactional outbox pattern to guarantee delivery.
        eventPublisher.publishOrderPlaced(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /** Called when payment-service reports the outcome of the split payment. */
    @Transactional
    public void applyPaymentResult(PaymentResultEvent event) {
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            // Kafka can deliver the same message more than once; ignore repeats (idempotent consumer)
            log.info("Order {} already {}, ignoring duplicate result", order.getId(), order.getStatus());
            return;
        }
        order.setStatus(event.outcome() == PaymentResultEvent.PaymentOutcome.COMPLETED
                ? OrderStatus.PAID
                : OrderStatus.PAYMENT_FAILED);
    }

    /** Split-payment rule: both parts must be positive and add up exactly to the order total. */
    void validateSplit(BigDecimal total, BigDecimal cardA, BigDecimal cardB) {
        if (cardA.signum() <= 0 || cardB.signum() <= 0) {
            throw new InvalidSplitPaymentException("Both payment amounts must be greater than zero");
        }
        if (cardA.add(cardB).compareTo(total) != 0) {
            throw new InvalidSplitPaymentException(
                    "Payment amounts (" + cardA.add(cardB) + ") must add up to the order total (" + total + ")");
        }
    }
}
