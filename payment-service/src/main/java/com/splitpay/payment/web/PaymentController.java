package com.splitpay.payment.web;

import com.splitpay.payment.dto.PaymentResponse;
import com.splitpay.payment.dto.SplitPaymentRequest;
import com.splitpay.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<PaymentResponse> record(@Valid @RequestBody SplitPaymentRequest request) {
        return paymentService.recordSplitPayment(request).stream().map(PaymentResponse::from).toList();
    }

    @GetMapping
    public List<PaymentResponse> forOrder(@RequestParam Long orderId) {
        return paymentService.getPaymentsForOrder(orderId).stream().map(PaymentResponse::from).toList();
    }
}
