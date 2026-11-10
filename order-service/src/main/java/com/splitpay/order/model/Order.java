package com.splitpay.order.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal totalAmount;
    private BigDecimal cardAAmount;   // first part of the split payment
    private BigDecimal cardBAmount;   // second part of the split payment

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant createdAt;

    protected Order() { }   // required by JPA

    public Order(List<OrderItem> items, BigDecimal totalAmount,
                 BigDecimal cardAAmount, BigDecimal cardBAmount) {
        this.items.addAll(items);
        this.totalAmount = totalAmount;
        this.cardAAmount = cardAAmount;
        this.cardBAmount = cardBAmount;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getCardAAmount() { return cardAAmount; }
    public BigDecimal getCardBAmount() { return cardBAmount; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
