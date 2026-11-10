package com.splitpay.payment.service;

import com.splitpay.payment.dto.SplitPaymentRequest;
import com.splitpay.payment.model.Payment;
import com.splitpay.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /** Records both parts of a split payment as PENDING. Processing comes on Day 2. */
    @Transactional
    public List<Payment> recordSplitPayment(SplitPaymentRequest request) {
        if (request.cardAAmount().add(request.cardBAmount()).compareTo(request.orderTotal()) != 0) {
            throw new InvalidSplitPaymentException("Payment amounts must add up to the order total");
        }
        if (!paymentRepository.findByOrderIdOrderBySplitPart(request.orderId()).isEmpty()) {
            // idempotency: the same order must not be charged twice (matters once Kafka can redeliver events)
            throw new InvalidSplitPaymentException("Payments already recorded for order " + request.orderId());
        }
        Payment partA = new Payment(request.orderId(), 1, request.cardALast4(), request.cardAAmount());
        Payment partB = new Payment(request.orderId(), 2, request.cardBLast4(), request.cardBAmount());
        return paymentRepository.saveAll(List.of(partA, partB));
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsForOrder(Long orderId) {
        return paymentRepository.findByOrderIdOrderBySplitPart(orderId);
    }
}
