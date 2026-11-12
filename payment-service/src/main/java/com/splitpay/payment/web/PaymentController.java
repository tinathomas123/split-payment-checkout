package com.splitpay.payment.web;

import com.splitpay.payment.dto.PaymentResponse;
import com.splitpay.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read-only now: payments are created from OrderPlaced events, not by REST calls. */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentResponse> forOrder(@RequestParam Long orderId) {
        return paymentService.getPaymentsForOrder(orderId).stream().map(PaymentResponse::from).toList();
    }
}
