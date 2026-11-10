package com.splitpay.payment.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** One row per part of a split payment: an order paid with two cards has two rows. */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private int splitPart;          // 1 = card A, 2 = card B
    private String cardLast4;       // never store the full card number
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private Instant createdAt;

    protected Payment() { }

    public Payment(Long orderId, int splitPart, String cardLast4, BigDecimal amount) {
        this.orderId = orderId;
        this.splitPart = splitPart;
        this.cardLast4 = cardLast4;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public int getSplitPart() { return splitPart; }
    public String getCardLast4() { return cardLast4; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(PaymentStatus status) { this.status = status; }
}
