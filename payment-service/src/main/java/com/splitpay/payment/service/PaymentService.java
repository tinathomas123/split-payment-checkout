package com.splitpay.payment.service;

import com.splitpay.payment.events.OrderPlacedEvent;
import com.splitpay.payment.events.PaymentResultEvent;
import com.splitpay.payment.events.PaymentResultEvent.PaymentOutcome;
import com.splitpay.payment.gateway.PaymentGateway;
import com.splitpay.payment.model.Payment;
import com.splitpay.payment.model.PaymentStatus;
import com.splitpay.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public PaymentService(PaymentRepository paymentRepository, PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Processes both parts of a split payment.
     * Charges card A, then card B. If card B is declined, card A is refunded so the customer
     * is never left half-charged. (Day 5 moves this saga into AWS Step Functions.)
     *
     * @return the result to publish, or empty if this order was already processed (duplicate event)
     */
    @Transactional
    public Optional<PaymentResultEvent> processSplitPayment(OrderPlacedEvent event) {
        if (paymentRepository.existsByOrderId(event.orderId())) {
            // Idempotency: Kafka can redeliver a message, but the customer must never be charged twice
            log.info("Payments for order {} already processed, skipping duplicate", event.orderId());
            return Optional.empty();
        }

        if (event.cardAAmount().add(event.cardBAmount()).compareTo(event.orderTotal()) != 0) {
            // Bad data is not worth retrying: fail the order straight away
            return Optional.of(new PaymentResultEvent(event.orderId(), PaymentOutcome.FAILED,
                    "Payment amounts do not add up to the order total"));
        }

        Payment partA = new Payment(event.orderId(), 1, event.cardALast4(), event.cardAAmount());
        Payment partB = new Payment(event.orderId(), 2, event.cardBLast4(), event.cardBAmount());

        // A ProviderUnavailableException thrown here rolls back the transaction, so nothing is
        // saved and the message can be retried safely.
        boolean aApproved = paymentGateway.charge(partA.getCardLast4(), partA.getAmount());
        partA.setStatus(aApproved ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED);

        if (!aApproved) {
            partB.setStatus(PaymentStatus.FAILED);   // card B is never charged
            paymentRepository.saveAll(List.of(partA, partB));
            return Optional.of(failed(event.orderId(), "Card ending " + partA.getCardLast4() + " declined"));
        }

        boolean bApproved = paymentGateway.charge(partB.getCardLast4(), partB.getAmount());
        partB.setStatus(bApproved ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED);

        if (!bApproved) {
            paymentGateway.refund(partA.getCardLast4(), partA.getAmount());   // compensating action
            partA.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.saveAll(List.of(partA, partB));
            return Optional.of(failed(event.orderId(),
                    "Card ending " + partB.getCardLast4() + " declined; card ending "
                            + partA.getCardLast4() + " refunded"));
        }

        paymentRepository.saveAll(List.of(partA, partB));
        return Optional.of(new PaymentResultEvent(event.orderId(), PaymentOutcome.COMPLETED, null));
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsForOrder(Long orderId) {
        return paymentRepository.findByOrderIdOrderBySplitPart(orderId);
    }

    private PaymentResultEvent failed(Long orderId, String reason) {
        return new PaymentResultEvent(orderId, PaymentOutcome.FAILED, reason);
    }
}
