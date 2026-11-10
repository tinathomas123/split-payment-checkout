package com.splitpay.order.service;

import com.splitpay.order.dto.CreateOrderRequest;
import com.splitpay.order.model.Order;
import com.splitpay.order.model.OrderItem;
import com.splitpay.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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

        Order order = new Order(items, total, request.cardAAmount(), request.cardBAmount());
        return orderRepository.save(order);
        // Day 2: publish an OrderPlaced event to Kafka here.
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /** Split-payment rule: both parts must be positive and add up exactly to the order total. */
    void validateSplit(BigDecimal total, BigDecimal cardA, BigDecimal cardB) {
        if (cardA.signum() <= 0 || cardB.signum() <= 0) {
            throw new InvalidSplitPaymentException("Both payment amounts must be greater than zero");
        }
        // compareTo, not equals: BigDecimal.equals treats 10.0 and 10.00 as different
        if (cardA.add(cardB).compareTo(total) != 0) {
            throw new InvalidSplitPaymentException(
                    "Payment amounts (" + cardA.add(cardB) + ") must add up to the order total (" + total + ")");
        }
    }
}
